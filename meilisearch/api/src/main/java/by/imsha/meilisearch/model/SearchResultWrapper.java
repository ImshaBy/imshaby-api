package by.imsha.meilisearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
//TODO заменить Object на конкретные типы
public record SearchResultWrapper(
        List<SearchResultItem> hits,
        Object facetDistribution,
        int processingTimeMs,
        String query,
        int offset,
        int limit,
        int estimatedTotalHits,
        Object facetStats) {
}
