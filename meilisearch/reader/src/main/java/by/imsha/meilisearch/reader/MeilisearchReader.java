package by.imsha.meilisearch.reader;

import by.imsha.meilisearch.model.SearchResultWrapper;

public interface MeilisearchReader {

    SearchResultWrapper search(QueryData queryData);
}
