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

//TODO javadoc
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
            final TaskInfo deleteDocumentsTaskInfo = meilisearchApiFeignClient.deleteDocuments(
                    indexUid,
                    DeleteDocumentsByFilterRequest.builder()
                            .filter("parish.id = " + parishId)
                            .build()
            );

            final Task deleteDocumentsTask = waitForTask(deleteDocumentsTaskInfo.getTaskUid(), 60000, 5000);
            if (deleteDocumentsTask.getStatus() != TaskStatus.SUCCEEDED) {
                throw new MeilisearchWriterException("Delete documents failed. Index uid = '%s'. Task = %s"
                        .formatted(indexUid, convertToJsonStringOrNull(deleteDocumentsTask)));
            }

            addDocumentsInBatch(getIndex(), searchRecords, 100);
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Can't refresh index data. Index uid = '%s'".formatted(indexUid), exception);
        }
    }

    @Override
    public void refreshAllData(final List<SearchRecord> searchRecords) {
        final Index tempIndex = dropAndCreateTempIndex();

        addDocumentsInBatch(tempIndex, searchRecords, 100);

        try {
            final TaskInfo swapIndexesTaskInfo = client.swapIndexes(
                    new SwapIndexesParams[]{
                            new SwapIndexesParams().setIndexes(
                                    new String[]{tempIndex.getUid(), indexUid}
                            )
                    });

            final Task task = waitForTask(swapIndexesTaskInfo.getTaskUid(), 60000, 5000);
            if (task.getStatus() != TaskStatus.SUCCEEDED) {
                throw new MeilisearchWriterException("Swap indexes failed. Index uids = ['%s', '%s']. Task = %s"
                        .formatted(indexUid, tempIndex.getUid(), convertToJsonStringOrNull(task)));
            }
        } catch (MeilisearchWriterException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Swap indexes failed. Index uids = ['%s', '%s']."
                    .formatted(indexUid, tempIndex.getUid()), exception);
        }

    }

    private Index createIndex(final String indexUid) throws MeilisearchException {
        final TaskInfo createIndexTask = client.createIndex(indexUid, PRIMARY_KEY_FIELD);

        client.waitForTask(createIndexTask.getTaskUid());

        final TaskInfo updateSettingsTask = client.getIndex(indexUid).updateSettings(settings);

        client.waitForTask(updateSettingsTask.getTaskUid());

        return client.getIndex(indexUid);
    }

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

    private Index dropAndCreateTempIndex() {
        final String tempIndexUid = indexUid + "Temp";

        try {
            final TaskInfo deleteTempIndexTask = client.deleteIndex(tempIndexUid);
            waitForTask(deleteTempIndexTask.getTaskUid(), 60000, 5000);
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Can't delete index with uid='%s'".formatted(tempIndexUid), exception);
        }

        try {
            return createIndex(tempIndexUid);
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Can't create index with uid='%s'".formatted(tempIndexUid), exception);
        }
    }

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
