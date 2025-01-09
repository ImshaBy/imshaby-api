package by.imsha.meilisearch.writer;

import by.imsha.meilisearch.model.SearchRecord;
import by.imsha.meilisearch.writer.exception.MeilisearchWriterException;
import by.imsha.meilisearch.writer.feign.MeilisearchApiFeignClient;
import by.imsha.meilisearch.writer.feign.dto.request.DeleteDocumentsByFilterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.DeleteTasksQuery;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.SwapIndexesParams;
import com.meilisearch.sdk.model.Task;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;
import com.meilisearch.sdk.model.TasksQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static by.imsha.meilisearch.model.SearchRecord.PRIMARY_KEY_FIELD;

/**
 * Стандартная реализация операций модификации индекса
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultMeilisearchWriter implements MeilisearchWriter {

    public static final String DOCUMENT_ADDITION_OR_UPDATE_TASK_TYPE = "documentAdditionOrUpdate";
    private final Client client;
    private final String indexUid;
    private final Settings settings;
    private final MeilisearchApiFeignClient meilisearchApiFeignClient;
    private final ObjectMapper objectMapper;

    @Override
    public void refreshParishData(final String parishId, final List<SearchRecord> searchRecords) {
        try {
            final Index index = getIndex();

            final TaskInfo deleteDocumentsTaskInfo = meilisearchApiFeignClient.deleteDocuments(
                    indexUid,
                    DeleteDocumentsByFilterRequest.builder()
                            .filter("parish.id = " + parishId)
                            .build()
            );

            waitForTask(deleteDocumentsTaskInfo.getTaskUid(), 60000, 500);

            addDocumentsInBatch(index, searchRecords, 100);
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Can't refresh index data. Index uid = '%s'".formatted(indexUid), exception);
        }
    }

    @Override
    public void refreshAllData(final List<SearchRecord> searchRecords) {
        final Index tempIndex = createTempIndex();

        try {
            addDocumentsInBatch(tempIndex, searchRecords, 1000);
        } catch (MeilisearchWriterException exception) {
            dropIndex(tempIndex.getUid(), true);
            throw exception;
        }

        try {
            final Index index = getIndex();

            final TaskInfo swapIndexesTaskInfo = client.swapIndexes(
                    new SwapIndexesParams[]{
                            new SwapIndexesParams().setIndexes(
                                    new String[]{tempIndex.getUid(), index.getUid()}
                            )
                    }
            );

            waitForTask(swapIndexesTaskInfo.getTaskUid(), 60000, 500);
        } catch (MeilisearchWriterException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Swap indexes failed. Index uids = ['%s', '%s']."
                    .formatted(indexUid, tempIndex.getUid()), exception);
        } finally {
            //удаляем временный индекс в "бесшумном" режиме, т.к. удаление после обмена не критично
            dropIndex(tempIndex.getUid(), true);
        }
    }

    /**
     * Создать индекс с заданным uid и имеющимися настройками
     *
     * @param indexUid uid создаваемого индекса
     * @return созданный индекс
     * @throws MeilisearchException в случае ошибки создания индекса
     */
    private Index createIndex(final String indexUid) throws MeilisearchException {
        final TaskInfo createIndexTask = client.createIndex(indexUid, PRIMARY_KEY_FIELD);

        client.waitForTask(createIndexTask.getTaskUid());

        final TaskInfo updateSettingsTask = client.getIndex(indexUid).updateSettings(settings);

        client.waitForTask(updateSettingsTask.getTaskUid());

        return client.getIndex(indexUid);
    }

    /**
     * Получить, либо создать индекс с имеющимися настройками
     *
     * @return индекс
     */
    private Index getIndex() {
        try {
            return client.getIndex(indexUid);
        } catch (MeilisearchApiException exception) {
            if ("index_not_found".equals(exception.getCode())) {
                log.info("Index with uid='{}' not found", indexUid, exception);
            } else {
                throw new MeilisearchWriterException("Unexpected error while fetching index. Index uid='%s', errorCode = '%s'"
                        .formatted(indexUid, exception.getCode()), exception);
            }
        } catch (MeilisearchException exception) {
            throw new MeilisearchWriterException("Unexpected error while fetching index. Index uid='%s'"
                    .formatted(indexUid), exception);
        }

        try {
            return createIndex(indexUid);
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Can't create index with uid='%s'".formatted(indexUid), exception);
        }
    }

    /**
     * Создать новый временный индекс
     *
     * @return чистый индекс с имеющимися настройками
     */
    private Index createTempIndex() {
        final String tempIndexUid = indexUid + "-" + UUID.randomUUID();

        try {
            return createIndex(tempIndexUid);
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Can't create index with uid='%s'".formatted(tempIndexUid), exception);
        }
    }

    /**
     * Удалить индекс
     *
     * @param indexUid Идентификатор удаляемого индекса
     * @param silent   признак необходимости логирования исключения вместо проброса далее
     */
    private void dropIndex(final String indexUid, boolean silent) {
        try {
            final TaskInfo deleteTempIndexTaskInfo = client.deleteIndex(indexUid);
            waitForTask(deleteTempIndexTaskInfo.getTaskUid(), 60000, 500);
        } catch (Exception exception) {
            if (silent) {
                log.info("Can't delete index with uid='{}'", indexUid, exception);
            } else {
                throw new MeilisearchWriterException("Can't delete index with uid='%s'".formatted(indexUid), exception);
            }
        }
    }

    /**
     * Пакетное сохранение документов
     *
     * @param index         индекс, в который добавляются документы
     * @param searchRecords добавляемые документы
     * @param batchSize     размер пакета
     */
    private void addDocumentsInBatch(final Index index, final List<SearchRecord> searchRecords, final int batchSize) {
        final int[] taskUids = IntStream.iterate(0, i -> i < searchRecords.size(), i -> i + batchSize)
                .mapToObj(i -> searchRecords.subList(i, Math.min(i + batchSize, searchRecords.size())))
                .parallel()
                .map(searchRecordsBatch -> {
                    try {
                        return index.addDocuments(objectMapper.writeValueAsString(searchRecordsBatch), PRIMARY_KEY_FIELD);
                    } catch (Exception exception) {
                        throw new MeilisearchWriterException("Add documents failed. Index uid = '%s'".formatted(indexUid), exception);
                    }
                })
                .mapToInt(TaskInfo::getTaskUid)
                .toArray();

        wailForAllTasksMatch(taskUids, new TasksQuery()
                        .setTypes(new String[]{DOCUMENT_ADDITION_OR_UPDATE_TASK_TYPE})
                        .setIndexUids(new String[]{index.getUid()})
                        .setStatuses(new String[]{TaskStatus.SUCCEEDED.taskStatus})
                        .setLimit(50),
                30_000, 500
        );
    }

    /**
     * Waits for a task to be processed
     *
     * @param taskUid      Identifier of the Task
     * @param timeoutInMs  number of milliseconds before throwing an Exception
     * @param intervalInMs number of milliseconds before requesting the status again
     */
    private void waitForTask(int taskUid, int timeoutInMs, int intervalInMs) {
        int[] taskUids = {taskUid};
        wailForAllTasksMatch(taskUids,
                new TasksQuery()
                        .setUids(taskUids)
                        .setStatuses(new String[]{TaskStatus.SUCCEEDED.taskStatus}),
                timeoutInMs,
                intervalInMs
        );
    }

    private void wailForAllTasksMatch(int[] taskUids, TasksQuery repeatableTasksQuery,
                                      int timeoutInMs, int intervalInMs) {
        long startTime = new Date().getTime();
        long elapsedTime = 0;

        Set<Integer> taskUidSet = new HashSet<>();
        for (int taskUid : taskUids) {
            taskUidSet.add(taskUid);
        }

        while (true) {
            if (elapsedTime >= timeoutInMs) {
                throw new MeilisearchWriterException("Waiting for tasks completion timed out. Task uids: %s. Query: %s."
                        .formatted(
                                Arrays.toString(taskUids),
                                repeatableTasksQuery
                        ));
            }

            try {
                removeProcessedTasks(taskUidSet, repeatableTasksQuery);
            } catch (MeilisearchException exception) {
                throw new MeilisearchWriterException(
                        "Error during waiting for tasks completion. Task uids: %s. Query: %s."
                                .formatted(
                                        Arrays.toString(taskUids),
                                        repeatableTasksQuery
                                ),
                        exception
                );
            }

            if (taskUidSet.isEmpty()) {
                break;
            }

            try {
                Thread.sleep(intervalInMs);
            } catch (Exception exception) {
                log.error("Thread sleep failed!", exception);
                Thread.currentThread().interrupt();
            }

            elapsedTime = new Date().getTime() - startTime;
        }
    }

    private void removeProcessedTasks(Set<Integer> taskUidSet, TasksQuery repeatableTasksQuery) throws MeilisearchException {
        int[] processedTaskUids = Arrays.stream(
                        this.client.getTasks(repeatableTasksQuery).getResults()
                )
                .mapToInt(Task::getUid)
                .toArray();

        for (int processedTaskUid : processedTaskUids) {
            taskUidSet.remove(processedTaskUid);
        }

        if (processedTaskUids.length != 0) {
            this.client.deleteTasks(
                    new DeleteTasksQuery()
                            .setUids(processedTaskUids)
            );
        }
    }
}
