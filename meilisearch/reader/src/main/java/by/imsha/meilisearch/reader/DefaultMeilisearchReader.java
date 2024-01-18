package by.imsha.meilisearch.reader;

import by.imsha.meilisearch.model.SearchResultWrapper;
import by.imsha.meilisearch.reader.exception.MeilisearchReaderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static by.imsha.meilisearch.model.SearchRecord.FilterableAttribute.CITY_ID;
import static by.imsha.meilisearch.model.SearchRecord.FilterableAttribute.LANG;
import static by.imsha.meilisearch.model.SearchRecord.FilterableAttribute.ONLINE;
import static by.imsha.meilisearch.model.SearchRecord.FilterableAttribute.PARISH_ID;
import static by.imsha.meilisearch.model.SearchRecord.FilterableAttribute.RORATE;

@Slf4j
@Getter
public class DefaultMeilisearchReader implements MeilisearchReader {

    public static final int DEFAULT_LIMIT = 300;
    private final Client client;
    private final Index index;
    private final ObjectMapper objectMapper;

    public DefaultMeilisearchReader(Client client, String indexUid, ObjectMapper objectMapper) throws MeilisearchException {
        this.client = client;
        this.index = client.index(indexUid);
        this.objectMapper = objectMapper;
    }

    @Override
    public SearchResultWrapper search(final QueryData queryData) {
        final SearchRequest searchRequest = SearchRequest.builder()
                .filterArray(queryData.toFilterArray())
                .facets(new String[]{CITY_ID, PARISH_ID, ONLINE, LANG, RORATE})
                .limit(DEFAULT_LIMIT)
                .build();

        try {
            //результат поиска (json, т.к. sdk не подходит со своей реализацией)
            final String rawSearchResult = index.rawSearch(searchRequest);

            return objectMapper.readValue(rawSearchResult, SearchResultWrapper.class);
        } catch (MeilisearchException exception) {
            throw new MeilisearchReaderException("Failed to search. Index uid = '%s'".formatted(index.getUid()),
                    exception);
        } catch (JsonProcessingException exception) {
            throw new MeilisearchReaderException("Failed read search response. Index uid = '%s'".formatted(index.getUid()),
                    exception);
        }
    }
}
