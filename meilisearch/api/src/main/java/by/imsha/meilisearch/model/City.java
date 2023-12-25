package by.imsha.meilisearch.model;

import lombok.Builder;

@Builder
public record City(
        String key,
        String name) {
}
