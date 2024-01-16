package by.imsha.meilisearch.writer;

import by.imsha.meilisearch.model.SearchRecord;

import java.util.List;

public interface MeilisearchWriter {

    void refreshParishData(String parishId, List<SearchRecord> searchRecords);

    void refreshAllData(List<SearchRecord> searchRecords);
}
