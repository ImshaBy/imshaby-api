package by.imsha.server.mapping.response.dto.webhook;

import by.imsha.server.FieldNameGetter;

public class WebHookResponseDtoMapping implements FieldNameGetter {

    public static final String IDENTIFIER_FIELD_QUALIFIER = "Идентификатор";
    public static final String TYPE_FIELD_QUALIFIER = "Тип";
    public static final String KEY_FIELD_QUALIFIER = "Ключ";
    public static final String URL_FIELD_QUALIFIER = "Url";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (IDENTIFIER_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "id";
        }
        if (TYPE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "type";
        }
        if (KEY_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "key";
        }
        if (URL_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "url";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа на получение WebHook");
    }
}