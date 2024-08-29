package by.imsha.server.mapping.response.dto.mass;

import by.imsha.server.FieldNameGetter;

public class MassResponseDtoMapping implements FieldNameGetter {

    public static final String IDENTIFIER_FIELD_QUALIFIER = "Идентификатор";
    public static final String CITY_ID_FIELD_QUALIFIER = "Идентификатор города";
    public static final String LANG_CODE_FIELD_QUALIFIER = "Код языка";
    public static final String DURATION_FIELD_QUALIFIER = "Продолжительность";
    public static final String TIME_FIELD_QUALIFIER = "Время";
    public static final String DAYS_FIELD_QUALIFIER = "Дни";
    public static final String ONLINE_FIELD_QUALIFIER = "Онлайн";
    public static final String RORATE_FIELD_QUALIFIER = "Рораты";
    public static final String PARISH_ID_FIELD_QUALIFIER = "Идентификатор парафии";
    public static final String DELETED_FIELD_QUALIFIER = "Удален";
    public static final String NOTES_FIELD_QUALIFIER = "Примечания";
    public static final String SINGLE_START_TIMESTAMP_FIELD_QUALIFIER = "Начало времени";
    public static final String LAST_MODIFIED_DATE_FIELD_QUALIFIER = "Дата последнего изменения";
    public static final String START_DATE_FIELD_QUALIFIER = "Дата начала";
    public static final String END_DATE_FIELD_QUALIFIER = "Дата окончания";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (IDENTIFIER_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "id";
        }
        if (CITY_ID_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "cityId";
        }
        if (LANG_CODE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "langCode";
        }
        if (DURATION_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "duration";
        }
        if (TIME_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "time";
        }
        if (DAYS_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "days";
        }
        if (ONLINE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "online";
        }
        if (RORATE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "rorate";
        }
        if (PARISH_ID_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "parishId";
        }
        if (DELETED_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "deleted";
        }
        if (NOTES_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "notes";
        }
        if (SINGLE_START_TIMESTAMP_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "singleStartTimestamp";
        }
        if (LAST_MODIFIED_DATE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "lastModifiedDate";
        }
        if (START_DATE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "startDate";
        }
        if (END_DATE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "endDate";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа службы");
    }
}