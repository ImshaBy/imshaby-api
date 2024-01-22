package by.imsha.meilisearch.model;

import by.imsha.meilisearch.serialization.LocalDateTime2TimestampSerializer;
import by.imsha.meilisearch.serialization.Seconds2LocalTimeDeserializer;
import by.imsha.meilisearch.serialization.Timestamp2LocalDateDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Представление записи в индексе, при поиске
 * <p>
 * Отличается полем geoDistance
 */
public record SearchResultItem(
        String recordId,
        String massId,
        Integer duration,
        @JsonDeserialize(using = Seconds2LocalTimeDeserializer.class)
        LocalTime time,
        @JsonDeserialize(using = Timestamp2LocalDateDeserializer.class)
        LocalDate date,
        Parish parish,
        String notes,
        String lang,
        Boolean online,
        Boolean rorate,
        City city,
        Boolean needUpdate,
        @JsonSerialize(using = LocalDateTime2TimestampSerializer.class)
        LocalDateTime lastModifiedDate,
        @JsonProperty("_geo")
        Geo geo,
        @JsonProperty("_geoDistance")
        Long geoDistance) {
}
