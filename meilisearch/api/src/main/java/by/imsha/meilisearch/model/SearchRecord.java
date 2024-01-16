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

    /**
     * Название ключевого поля записи
     */
    public static final String PRIMARY_KEY_FIELD = "recordId";
    /**
     * Атрибуты по которым возможна фильтрация
     */
    public static final String[] FILTERABLE_ATTRIBUTES = new String[]{
            "_geo", //для сортировки по _geo
            "parish.name", //для поиска и фасетов
            "city.name",
            "online",
            "lang"
    };
    /**
     * Атрибуты по которым возможна сортировка
     */
    public static final String[] SORTABLE_ATTRIBUTES = new String[]{"_geo"};
    /**
     * Атрибуты использующиеся при поиске
     */
    public static final String[] SEARCHABLE_ATTRIBUTES = new String[]{"parish.key"};
    /**
     * Атрибуты отображаемые в документах (* - все атрибуты)
     */
    public static final String[] DISPLAYED_ATTRIBUTES = new String[]{"*"};
}
