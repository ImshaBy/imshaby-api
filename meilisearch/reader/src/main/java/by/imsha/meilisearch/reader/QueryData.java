package by.imsha.meilisearch.reader;

public record QueryData(
        String cityId,
        String parishId,
        String lang,
        Boolean online,
        Boolean rorate
) {
}
