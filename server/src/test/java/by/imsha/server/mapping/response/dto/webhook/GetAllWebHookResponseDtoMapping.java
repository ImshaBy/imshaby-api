package by.imsha.server.mapping.response.dto.webhook;

import by.imsha.server.FieldNameGetter;

public class GetAllWebHookResponseDtoMapping implements FieldNameGetter {

    public static final String CONTENT_FIELD_QUALIFIER = "Контент";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (CONTENT_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "content";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа на получение списка WebHook");
    }
}