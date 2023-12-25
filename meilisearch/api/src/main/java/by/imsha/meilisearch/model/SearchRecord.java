package by.imsha.meilisearch.model;

import by.imsha.meilisearch.serialization.LocalDate2TimestampSerializer;
import by.imsha.meilisearch.serialization.LocalDateTime2TimestampSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record SearchRecord(
        String recordId,
        @JsonSerialize(using = LocalDateTime2TimestampSerializer.class)
        LocalDateTime time,
        @JsonSerialize(using = LocalDate2TimestampSerializer.class)
        LocalDate date,
        Parish parish,
        String notes,
        String lang,
        Boolean online,
        Boolean rorate,
        City city,
        Boolean needUpdate,
        @JsonProperty("_geo")
        Geo geo) {
}
