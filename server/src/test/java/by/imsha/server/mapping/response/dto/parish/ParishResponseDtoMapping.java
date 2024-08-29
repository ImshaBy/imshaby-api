package by.imsha.server.mapping.response.dto.parish;

import by.imsha.server.FieldNameGetter;

public class ParishResponseDtoMapping implements FieldNameGetter {

    public static final String IDENTIFIER_FIELD_QUALIFIER = "Идентификатор";
    public static final String STATE_FIELD_QUALIFIER = "Статус";
    public static final String IMG_PATH_FIELD_QUALIFIER = "Расположение изображения";
    public static final String BROADCAST_URL_FIELD_QUALIFIER = "Адрес трансляции";
    public static final String USER_ID_FIELD_QUALIFIER = "Идентификатор пользователя";
    public static final String NAME_FIELD_QUALIFIER = "Наименование";
    public static final String SHORT_NAME_FIELD_QUALIFIER = "Краткое наименование";
    public static final String ADDRESS_FIELD_QUALIFIER = "Адрес";
    public static final String GPS_LATITUDE_FIELD_QUALIFIER = "GPS широта";
    public static final String GPS_LONGITUDE_FIELD_QUALIFIER = "GPS долгота";
    public static final String KEY_FIELD_QUALIFIER = "Ключ";
    public static final String UPDATE_PERIOD_IN_DAYS_FIELD_QUALIFIER = "Период обновления в днях";
    public static final String NEED_UPDATE_FIELD_QUALIFIER = "Нужно обновление";
    public static final String CITY_ID_FIELD_QUALIFIER = "Идентификатор города";
    public static final String PHONE_FIELD_QUALIFIER = "Телефон";
    public static final String SUPPORT_PHONE_FIELD_QUALIFIER = "Телефон поддержки";
    public static final String EMAIL_FIELD_QUALIFIER = "Почта";
    public static final String LAST_MODIFIED_EMAIL_FIELD_QUALIFIER = "Почта последнего изменнения";
    public static final String WEBSITE_FIELD_QUALIFIER = "Веб-сайт";
    public static final String LAST_MASS_ACTUAL_DATE_FIELD_QUALIFIER = "Дата актуальности Служб";
    public static final String LAST_MODIFIED_DATE_FIELD_QUALIFIER = "Дата последнего изменения";
    public static final String LAST_CONFIRM_RELEVANCE_FIELD_QUALIFIER = "Дата последнего подтверждение актуальности";
    public static final String LOCALIZED_INFO_FIELD_QUALIFIER = "Локализация";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (IDENTIFIER_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "id";
        }
        if (STATE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "state";
        }
        if (IMG_PATH_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "imgPath";
        }
        if (BROADCAST_URL_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "broadcastUrl";
        }
        if (USER_ID_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "userId";
        }
        if (NAME_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "name";
        }
        if (SHORT_NAME_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "shortName";
        }
        if (ADDRESS_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "address";
        }
        if (GPS_LATITUDE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "gps.latitude";
        }
        if (GPS_LONGITUDE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "gps.longitude";
        }
        if (KEY_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "key";
        }
        if (UPDATE_PERIOD_IN_DAYS_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "updatePeriodInDays";
        }
        if (NEED_UPDATE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "needUpdate";
        }
        if (CITY_ID_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "cityId";
        }
        if (PHONE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "phone";
        }
        if (SUPPORT_PHONE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "supportPhone";
        }
        if (EMAIL_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "email";
        }
        if (LAST_MODIFIED_EMAIL_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "lastModifiedEmail";
        }
        if (WEBSITE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "website";
        }
        if (LAST_MASS_ACTUAL_DATE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "lastMassActualDate";
        }
        if (LAST_MODIFIED_DATE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "lastModifiedDate";
        }
        if (LAST_CONFIRM_RELEVANCE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "lastConfirmRelevance";
        }
        if (LOCALIZED_INFO_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "localizedInfo";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа на получение парафии по идентификатору пользователя");
    }
}