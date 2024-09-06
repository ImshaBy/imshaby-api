package by.imsha.meilisearch.reader;

import by.imsha.meilisearch.model.SearchResultItem;
import by.imsha.meilisearch.reader.exception.MeilisearchReaderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static by.imsha.meilisearch.model.SearchRecord.FilterableAttribute.LANG;
import static by.imsha.meilisearch.model.SearchRecord.FilterableAttribute.ONLINE;
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
    public SearchResult search(final QueryData queryData) {
        final int limit = DEFAULT_LIMIT;
        final SearchRequest.SearchRequestBuilder searchRequestTemplate = SearchRequest.builder()
                .filterArray(queryData.toFilterArray())
                .facets(new String[]{ONLINE, LANG, RORATE})
                .limit(limit);

        //загружаем все страницы
        searchRequestTemplate.offset(0);
        final SearchResultWrapper resultWrapper = search(searchRequestTemplate.build());

        int limitUsed = limit;
        int estimatedHits = resultWrapper.estimatedTotalHits();

        if (limitUsed < estimatedHits) {
            //если НЕ ВСЕ документы были загружены в первом запросе
            searchRequestTemplate.facets(null);//для следующих (после первой) загрузок не нужны распределения
            final List<SearchResultItem> allHits = new ArrayList<>(estimatedHits);
            allHits.addAll(resultWrapper.hits());//результат первого запроса

            do {
                searchRequestTemplate.offset(limitUsed);
                final SearchResultWrapper additionalSearchResultWrapper = search(searchRequestTemplate.build());
                allHits.addAll(additionalSearchResultWrapper.hits());

                limitUsed += limit;
            } while (limitUsed < estimatedHits);

            return SearchResult.builder()
                    .hits(allHits)
                    .facetDistribution(resultWrapper.facetDistribution())
                    .build();
        } else {
            //если все документы были загружены в первом запросе
            return SearchResult.builder()
                    .hits(resultWrapper.hits())
                    .facetDistribution(resultWrapper.facetDistribution())
                    .build();
        }
    }

    private SearchResultWrapper search(final SearchRequest searchRequest) {
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
