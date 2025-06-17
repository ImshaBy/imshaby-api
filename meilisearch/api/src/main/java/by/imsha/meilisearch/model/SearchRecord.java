package by.imsha.meilisearch.model;

import by.imsha.meilisearch.serialization.LocalDateTime2TimestampSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

/**
 * Запись в индексе (используется при сохранении в индекс)
 *
 * @param recordId         идентификатор записи
 * @param massId           идентификатор мессы
 * @param duration         длительность мессы
 * @param dateTime         дата и время мессы
 * @param parish           данные парафии
 * @param notes            заметки (доп.инфо)
 * @param lang             язык мессы
 * @param online           признак трансляции мессы в онлайне
 * @param rorate           признак рорат
 * @param city             данные города
 * @param geo              данные геолокации парафии
 * @param lastModifiedDate дата и время последней модификации
 */
@Builder
public record SearchRecord(
        String recordId,
        String massId,
        Integer duration,
        @JsonSerialize(using = LocalDateTime2TimestampSerializer.class)
        LocalDateTime dateTime,
        Parish parish,
        //TODO пока что у нас только в BY заметки
        String notes,
        String lang,
        Boolean online,
        Boolean rorate,
        City city,
        @JsonSerialize(using = LocalDateTime2TimestampSerializer.class)
        LocalDateTime lastModifiedDate,
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
            FilterableAttribute.DATE_TIME,
            FilterableAttribute.GEO, //для сортировки по _geo
            FilterableAttribute.PARISH_ID, //для фильтров и фасетов
            FilterableAttribute.CITY_ID,
            FilterableAttribute.ONLINE,
            FilterableAttribute.LANG,
            FilterableAttribute.RORATE
    };
    /**
     * Атрибуты по которым возможна сортировка
     */
    public static final String[] SORTABLE_ATTRIBUTES = new String[]{
            SortableAttribute.DATE_TIME,
            SortableAttribute.GEO
    };
    /**
     * Атрибуты использующиеся при поиске
     */
    public static final String[] SEARCHABLE_ATTRIBUTES = new String[]{};
    /**
     * Атрибуты отображаемые в документах (* - все атрибуты)
     */
    public static final String[] DISPLAYED_ATTRIBUTES = new String[]{"*"};

    @UtilityClass
    public static class RecordAttribute {
        public static final String RECORD_ID = "recordId";
        public static final String MASS_ID = "massId";
        public static final String DURATION = "duration";
        public static final String DATE_TIME = "dateTime";
        public static final String PARISH = "parish";
        public static final String PARISH_ID = "parish.id";
        public static final String PARISH_ACTUAL = "parish.actual";
        public static final String PARISH_STATE = "parish.state";
        public static final String NOTES = "notes";
        public static final String LANG = "lang";
        public static final String ONLINE = "online";
        public static final String RORATE = "rorate";
        public static final String CITY = "city";
        public static final String CITY_ID = "city.id";
        public static final String LAST_MODIFIED_DATE = "lastModifiedDate";
        public static final String GEO = "_geo";
    }

    @UtilityClass
    public static class FilterableAttribute {
        public static final String DATE_TIME = RecordAttribute.DATE_TIME;
        public static final String CITY_ID = RecordAttribute.CITY_ID;
        public static final String PARISH_ID = RecordAttribute.PARISH_ID;
        public static final String ONLINE = RecordAttribute.ONLINE;
        public static final String LANG = RecordAttribute.LANG;
        public static final String RORATE = RecordAttribute.RORATE;
        public static final String GEO = RecordAttribute.GEO;

    }

    @UtilityClass
    public static class SortableAttribute {
        public static final String GEO = RecordAttribute.GEO;
        public static final String DATE_TIME = RecordAttribute.DATE_TIME;
        public static final String DATE_TIME_ASC = RecordAttribute.DATE_TIME + ":asc";
        public static final String DATE_TIME_DESC = RecordAttribute.DATE_TIME + ":desc";
    }

}
