package by.imsha.meilisearch.model;

import lombok.Builder;

@Builder
public record Geo(
        Double lat,
        Double lng) {
}
