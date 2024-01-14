package by.imsha.meilisearch.writer;

import by.imsha.meilisearch.model.SearchRecord;

import java.util.Collection;

public interface MeilisearchWriter {

    void refreshParishData(String parishId, Collection<SearchRecord> searchRecords);
}
