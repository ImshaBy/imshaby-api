package by.imsha.meilisearch.reader;

import by.imsha.meilisearch.model.SearchResultWrapper;
import by.imsha.meilisearch.reader.exception.MeilisearchReaderException;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public class DefaultMeilisearchReader implements MeilisearchReader {

    private final Client client;
    private final String indexUid;

    @Override
    public SearchResultWrapper search(final QueryData queryData) {

        return null;
    }

    /**
     * TODO проверить!!!
     *  возможно нет необходимости получать каждый раз новый индекс,
     *  а просто пользоваться полученным однажды
     */
    private Index getIndex(final Client client) {
        try {
            return client.getIndex(indexUid);
        } catch (Exception exception) {
            throw new MeilisearchReaderException("Can't get index with uid='" + indexUid + "'", exception);
        }
    }
}
