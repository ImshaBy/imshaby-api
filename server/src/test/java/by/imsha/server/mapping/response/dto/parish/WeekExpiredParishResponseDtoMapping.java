package by.imsha.server.mapping.response.dto.parish;

import by.imsha.server.FieldNameGetter;

public class WeekExpiredParishResponseDtoMapping implements FieldNameGetter {

    public static final String KEY_FIELD_QUALIFIER = "Ключ";
    public static final String NAME_FIELD_QUALIFIER = "Наименование";
    public static final String IDENTIFIER_FIELD_QUALIFIER = "Идентификатор";
    public static final String UPDATE_PERIOD_IN_DAYS_FIELD_QUALIFIER = "Период обновления в днях";
    public static final String SUPPORT_PHONE_FIELD_QUALIFIER = "Телефон поддержки";
    public static final String PHONE_FIELD_QUALIFIER = "Телефон";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (KEY_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "key";
        }
        if (NAME_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "name";
        } if (IDENTIFIER_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "id";
        }
        if (UPDATE_PERIOD_IN_DAYS_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "updatePeriodInDays";
        }
        if (SUPPORT_PHONE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "supportPhone";
        }
        if (PHONE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "phone";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа на получение парафии по идентификатору пользователя");
    }
}