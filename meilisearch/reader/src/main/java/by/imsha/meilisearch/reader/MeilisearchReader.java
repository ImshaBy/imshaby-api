package by.imsha.meilisearch.reader;

public interface MeilisearchReader {

    SearchResult searchAllMasses(MassSearchFilter queryData);

    SearchResult searchNearestMasses(MassSearchFilter queryData);
}
