package by.imsha.meilisearch.model;

import by.imsha.meilisearch.serialization.Timestamp2LocalDateDeserializer;
import by.imsha.meilisearch.serialization.Timestamp2LocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Представление записи в индексе, при поиске
 * <p>
 * Отличается полем geoDistance
 */
public record SearchResultItem(
        String recordId,
        @JsonDeserialize(using = Timestamp2LocalDateTimeDeserializer.class)
        LocalDateTime time,
        @JsonDeserialize(using = Timestamp2LocalDateDeserializer.class)
        LocalDate date,
        Parish parish,
        String notes,
        String lang,
        Boolean online,
        Boolean rorate,
        City city,
        Boolean needUpdate,
        @JsonProperty("_geo")
        Geo geo,
        @JsonProperty("_geoDistance")
        Long geoDistance) {
}
