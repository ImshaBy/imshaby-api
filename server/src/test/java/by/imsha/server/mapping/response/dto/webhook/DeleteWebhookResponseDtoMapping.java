package by.imsha.server.mapping.response.dto.webhook;

import by.imsha.server.FieldNameGetter;

public class DeleteWebhookResponseDtoMapping implements FieldNameGetter {

    public static final String IDENTIFIER_FIELD_QUALIFIER = "Идентификатор";
    public static final String STATUS_FIELD_QUALIFIER = "Статус";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (IDENTIFIER_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "id";
        }
        if (STATUS_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "status";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа удаления WebHook");
    }
}