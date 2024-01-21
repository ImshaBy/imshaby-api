package by.imsha.meilisearch.reader;

import by.imsha.meilisearch.model.SearchResultItem;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record SearchResult(
        List<SearchResultItem> hits,
        Map<String, Map<String, Integer>> facetDistribution
) {
}
