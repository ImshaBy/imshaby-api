package by.imsha.meilisearch.model;

import by.imsha.meilisearch.serialization.LocalDate2TimestampSerializer;
import by.imsha.meilisearch.serialization.LocalDateTime2TimestampSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Запись в индексе (используется при сохранении в индекс)
 *
 * @param recordId   идентификатор записи
 * @param time       дата и время мессы
 * @param date       дата мессы
 * @param parish     данные парафии
 * @param notes      заметки (доп.инфо)
 * @param lang       язык мессы
 * @param online     признак трансляции мессы в онлайне
 * @param rorate     признак рорат
 * @param city       данные города
 * @param needUpdate признак необходимости обновления данных
 * @param geo        данные геолокации парафии
 */
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
