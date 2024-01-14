package by.imsha.meilisearch.writer;

import by.imsha.meilisearch.model.SearchRecord;
import by.imsha.meilisearch.writer.exception.MeilisearchWriterException;
import by.imsha.meilisearch.writer.feign.MeilisearchApiFeignClient;
import by.imsha.meilisearch.writer.feign.dto.request.DeleteDocumentsByFilterRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.exceptions.MeilisearchTimeoutException;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.Task;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
public class DefaultMeilisearchWriter implements MeilisearchWriter {

    private final Client client;
    private final String indexUid;
    private final Settings settings;
    private final MeilisearchApiFeignClient meilisearchApiFeignClient;
    private final ObjectMapper objectMapper;

    private Index getIndex() {
        try {
            return client.getIndex(indexUid);
        } catch (Exception exception) {
            log.warn("Can't get index with uid='{}'", indexUid, exception);
        }

        try {
            final TaskInfo createIndexTask = client.createIndex(indexUid, SearchRecord.PRIMARY_KEY_FIELD);

            client.waitForTask(createIndexTask.getTaskUid());

            final TaskInfo updateSettingsTask = client.getIndex(indexUid).updateSettings(settings);

            client.waitForTask(updateSettingsTask.getTaskUid());

            return client.getIndex(indexUid);
        } catch (Exception exception) {
            throw new MeilisearchWriterException("Can't create or update index with uid='" + indexUid + "'", exception);
        }
    }

    @Override
    public void refreshParishData(final String parishId, final Collection<SearchRecord> searchRecords) {
        try {
            final TaskInfo deleteDocumentsTaskInfo = meilisearchApiFeignClient.deleteDocuments(
                    indexUid,
                    DeleteDocumentsByFilterRequest.builder()
                            .filter("parish.id = " + parishId)
                            .build()
            );

            waitForTask(deleteDocumentsTaskInfo.getTaskUid(), 60000, 5000);

            //есть вариант батча, но он делает лишнюю конвертацию
            //возможно стоит вообще отказаться от модификации и всегда менять копию, а затем делать swap
            final TaskInfo addDocumentsTaskInfo = getIndex().addDocuments(objectMapper.writeValueAsString(searchRecords),
                    SearchRecord.PRIMARY_KEY_FIELD);

            waitForTask(addDocumentsTaskInfo.getTaskUid(), 60000, 5000);
        } catch (MeilisearchException | JsonProcessingException e) {
            throw new MeilisearchWriterException("Can't refresh index data", e);
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
    private void waitForTask(int taskUid, int timeoutInMs, int intervalInMs) throws MeilisearchException {
        Task task;
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
                throw new MeilisearchTimeoutException();
            }
            elapsedTime = new Date().getTime() - startTime;
        }
    }
}
