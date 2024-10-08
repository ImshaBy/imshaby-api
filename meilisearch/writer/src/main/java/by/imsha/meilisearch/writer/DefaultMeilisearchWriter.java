package by.imsha.meilisearch.writer;

import by.imsha.meilisearch.model.SearchRecord;
import by.imsha.meilisearch.writer.exception.MeilisearchWriterException;
import by.imsha.meilisearch.writer.feign.MeilisearchApiFeignClient;
import by.imsha.meilisearch.writer.feign.dto.request.DeleteDocumentsByFilterRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.exceptions.MeilisearchTimeoutException;
import com.meilisearch.sdk.model.IndexesQuery;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.SwapIndexesParams;
import com.meilisearch.sdk.model.Task;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static by.imsha.meilisearch.model.SearchRecord.PRIMARY_KEY_FIELD;

/**
 * Стандартная реализация операций модификации индекса
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultMeilisearchWriter implements MeilisearchWriter {

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

            final Task deleteDocumentsTask = waitForTask(deleteDocumentsTaskInfo.getTaskUid(), 60000, 1000);
            if (deleteDocumentsTask.getStatus() != TaskStatus.SUCCEEDED) {
                throw new MeilisearchWriterException("Delete documents failed. Index uid = '%s'. Task = %s"
                        .formatted(indexUid, convertToJsonStringOrNull(deleteDocumentsTask)));
            }

            addDocumentsInBatch(index, searchRecords, 100);
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Can't refresh index data. Index uid = '%s'".formatted(indexUid), exception);
        }
    }

    @Override
    public void refreshAllData(final List<SearchRecord> searchRecords) {
        final Index tempIndex = dropAndCreateTempIndex();

        addDocumentsInBatch(tempIndex, searchRecords, 100);

        try {
            final Index index = getIndex();

            final TaskInfo swapIndexesTaskInfo = client.swapIndexes(
                    new SwapIndexesParams[]{
                            new SwapIndexesParams().setIndexes(
                                    new String[]{tempIndex.getUid(), index.getUid()}
                            )
                    });

            final Task task = waitForTask(swapIndexesTaskInfo.getTaskUid(), 60000, 1000);
            if (task.getStatus() != TaskStatus.SUCCEEDED) {
                throw new MeilisearchWriterException("Swap indexes failed. Index uids = ['%s', '%s']. Task = %s"
                        .formatted(index.getUid(), tempIndex.getUid(), convertToJsonStringOrNull(task)));
            }

            //удаляем временный индекс в "бесшумном" режиме, т.к. удаление после обмена не критично
            dropIndex(tempIndex.getUid(), true);
        } catch (MeilisearchWriterException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Swap indexes failed. Index uids = ['%s', '%s']."
                    .formatted(indexUid, tempIndex.getUid()), exception);
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
     * Удалить и создать новый временный индекс
     *
     * @return чистый индекс с имеющимися настройками
     */
    private Index dropAndCreateTempIndex() {
        final String tempIndexUid = indexUid + "-temp";
        //удаляем индекс в обычном режиме (в случае если он не был удален - исключение)
        dropIndex(tempIndexUid, false);

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
            final Task deleteTempIndexTask = waitForTask(deleteTempIndexTaskInfo.getTaskUid(), 60000, 1000);

            if (deleteTempIndexTask.getStatus() != TaskStatus.SUCCEEDED && checkIndexExists(indexUid)) {
                if (silent) {
                    log.info("Failed to delete index {}. Task = '{}'", indexUid, convertToJsonStringOrNull(deleteTempIndexTask));
                } else {
                    throw new MeilisearchWriterException("Can't delete index with uid='%s'. Task = '%s'".formatted(indexUid,
                            convertToJsonStringOrNull(deleteTempIndexTask)));
                }
            }
        } catch (Exception exception) {
            if (silent) {
                log.info("Can't delete index with uid='{}'", indexUid, exception);
            } else {
                throw new MeilisearchWriterException("Can't delete index with uid='%s'".formatted(indexUid), exception);
            }
        }
    }

    /**
     * Проверить существование индекса
     *
     * @param indexUid идентификатор индекса
     * @return {@code true} - если индекс существует, {@code false} - иначе
     */
    private boolean checkIndexExists(final String indexUid) {
        try {
            for (Index index : client.getIndexes(new IndexesQuery()).getResults()) {
                if (indexUid.equals(index.getUid())) {
                    return true;
                }
            }
            return false;
        } catch (MeilisearchException exception) {
            throw new MeilisearchWriterException("Can't get indexes", exception);
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
                .map(searchRecordsBatch -> {
                    try {
                        return index.addDocuments(objectMapper.writeValueAsString(searchRecordsBatch), PRIMARY_KEY_FIELD);
                    } catch (Exception exception) {
                        throw new MeilisearchWriterException("Add documents failed. Index uid = '%s'".formatted(indexUid), exception);
                    }
                })
                .mapToInt(TaskInfo::getTaskUid)
                .toArray();

        for (int taskUid : taskUids) {
            final Task task;
            try {
                task = waitForTask(taskUid, 60000, 5000);
            } catch (Exception exception) {
                throw new MeilisearchWriterException("Add documents failed. Index uid = '%s'. Task uid = '%d'"
                        .formatted(indexUid, taskUid));
            }

            if (task.getStatus() != TaskStatus.SUCCEEDED) {
                throw new MeilisearchWriterException("Add documents failed. Index uid = '%s'. Task = %s"
                        .formatted(indexUid, convertToJsonStringOrNull(task)));
            }
        }
    }

    /**
     * FIXME взял пока реализацию из meilisearch sdk
     * <p>
     * Waits for a task to be processed
     *
     * @param taskUid      Identifier of the Task
     * @param timeoutInMs  number of milliseconds before throwing an Exception
     * @param intervalInMs number of milliseconds before requesting the status again
     * @throws MeilisearchException if timeout is reached
     */
    private Task waitForTask(int taskUid, int timeoutInMs, int intervalInMs) throws MeilisearchException {
        Task task = null;
        TaskStatus status = null;
        long startTime = new Date().getTime();
        long elapsedTime = 0;

        while (status == null
                || (!status.equals(TaskStatus.SUCCEEDED) && !status.equals(TaskStatus.FAILED))) {
            if (elapsedTime >= timeoutInMs) {
                throw new MeilisearchTimeoutException();
            }
            task = this.client.getTask(taskUid);
            status = task.getStatus();
            try {
                Thread.sleep(intervalInMs);
            } catch (Exception e) {
                throw new MeilisearchTimeoutException(e);
            }
            elapsedTime = new Date().getTime() - startTime;
        }

        return task;
    }

    private String convertToJsonStringOrNull(final Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to json. Object - {}", object);
            return null;
        }
    }
}
