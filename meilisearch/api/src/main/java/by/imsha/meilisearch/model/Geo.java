package by.imsha.meilisearch.model;

import lombok.Builder;

/**
 * Данные геолокации (стандартная структура в meilisearch)
 *
 * @param lat широта
 * @param lng долгота
 */
@Builder
public record Geo(
        Double lat,
        Double lng) {
}
