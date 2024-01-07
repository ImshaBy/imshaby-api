package by.imsha.meilisearch.writer;

import by.imsha.meilisearch.model.SearchRecord;
import by.imsha.meilisearch.writer.exception.MeilisearchWriterException;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.TaskInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultMeilisearchWriter implements MeilisearchWriter {

    private final Client client;
    private final String indexUid;
    private final Settings settings;

    private Index getIndex(final Client client) {
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
}
