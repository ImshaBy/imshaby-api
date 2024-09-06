package by.imsha.server.mapping.request.dto.parish;

import by.imsha.server.FieldValueSetter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CreateParishDtoMapping implements FieldValueSetter {

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

    private String id;
    private String state;
    private String imgPath;
    private String broadcastUrl;
    private String userId;
    private String name;
    private String shortName;
    private String address;
    private String key;
    private Integer updatePeriodInDays;
    //TODO нужно ли нам это поле в тестах?
//    private Map<String, LocalizedBaseInfo> localizedInfo = new HashMap<>();
    private boolean needUpdate;
    private String cityId;
    private String phone;
    private String supportPhone;
    private String email;
    private String lastModifiedEmail;
    private String website;
    private String lastMassActualDate;
    private String lastModifiedDate;
    private String  lastConfirmRelevance;

    @Override
    public ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue) {
        if (fieldName.equals(IDENTIFIER_FIELD_QUALIFIER)) {
            return jsonNode.put("id", (String) fieldValue);
        }
        if (fieldName.equals(STATE_FIELD_QUALIFIER)) {
            return jsonNode.put("state", (String) fieldValue);
        }
        if (fieldName.equals(IMG_PATH_FIELD_QUALIFIER)) {
            return jsonNode.put("imgPath", (String) fieldValue);
        }
        if (fieldName.equals(BROADCAST_URL_FIELD_QUALIFIER)) {
            return jsonNode.put("broadcastUrl", (String) fieldValue);
        }
        if (fieldName.equals(USER_ID_FIELD_QUALIFIER)) {
            return jsonNode.put("userId", (String) fieldValue);
        }
        if (fieldName.equals(NAME_FIELD_QUALIFIER)) {
            return jsonNode.put("name", (String) fieldValue);
        }
        if (fieldName.equals(SHORT_NAME_FIELD_QUALIFIER)) {
            return jsonNode.put("shortName", (String) fieldValue);
        }
        if (fieldName.equals(ADDRESS_FIELD_QUALIFIER)) {
            return jsonNode.put("address", (String) fieldValue);
        }
        if (fieldName.equals(GPS_LATITUDE_FIELD_QUALIFIER)) {
            if (jsonNode.has("gps")) {
                ObjectNode gps = (ObjectNode) jsonNode.path("gps");
                gps.put("longitude", (String) fieldValue);
                jsonNode.put("gps", gps);
            } else {
                jsonNode.putObject("gps").put("latitude", (String) fieldValue);
            }
            return jsonNode;
        }
        if (fieldName.equals(GPS_LONGITUDE_FIELD_QUALIFIER)) {
            if (jsonNode.has("gps")) {
                ObjectNode gps = (ObjectNode) jsonNode.path("gps");
                gps.put("longitude", (String) fieldValue);
                jsonNode.put("gps", gps);
            } else {
                jsonNode.putObject("gps").put("longitude", (String) fieldValue);;
            }
            return jsonNode;
        }
        if (fieldName.equals(KEY_FIELD_QUALIFIER)) {
            return jsonNode.put("key", (String) fieldValue);
        }
        if (fieldName.equals(UPDATE_PERIOD_IN_DAYS_FIELD_QUALIFIER)) {
            return jsonNode.put("updatePeriodInDays", (String) fieldValue);
        }
        if (fieldName.equals(NEED_UPDATE_FIELD_QUALIFIER)) {
            return jsonNode.put("needUpdate", (String) fieldValue);
        }
        if (fieldName.equals(CITY_ID_FIELD_QUALIFIER)) {
            return jsonNode.put("cityId", (String) fieldValue);
        }
        if (fieldName.equals(PHONE_FIELD_QUALIFIER)) {
            return jsonNode.put("phone", (String) fieldValue);
        }
        if (fieldName.equals(SUPPORT_PHONE_FIELD_QUALIFIER)) {
            return jsonNode.put("supportPhone", (String) fieldValue);
        }
        if (fieldName.equals(EMAIL_FIELD_QUALIFIER)) {
            return jsonNode.put("email", (String) fieldValue);
        }
        if (fieldName.equals(LAST_MODIFIED_EMAIL_FIELD_QUALIFIER)) {
            return jsonNode.put("lastModifiedEmail", (String) fieldValue);
        }
        if (fieldName.equals(WEBSITE_FIELD_QUALIFIER)) {
            return jsonNode.put("website", (String) fieldValue);
        }
        if (fieldName.equals(LAST_MASS_ACTUAL_DATE_FIELD_QUALIFIER)) {
            return jsonNode.put("lastMassActualDate", (String) fieldValue);
        }
        if (fieldName.equals(LAST_MODIFIED_DATE_FIELD_QUALIFIER)) {
            return jsonNode.put("lastModifiedDate", (String) fieldValue);
        }
        if (fieldName.equals(LAST_CONFIRM_RELEVANCE_FIELD_QUALIFIER)) {
            return jsonNode.put("lastConfirmRelevance", (String) fieldValue);
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldName + "' в модели создания парафии");
    }
}