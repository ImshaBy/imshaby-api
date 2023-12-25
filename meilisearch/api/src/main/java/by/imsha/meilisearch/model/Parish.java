package by.imsha.meilisearch.model;

import lombok.Builder;

@Builder
public record Parish(
        String key,
        String name,
        String shortName,
        String imgPath) {
}
