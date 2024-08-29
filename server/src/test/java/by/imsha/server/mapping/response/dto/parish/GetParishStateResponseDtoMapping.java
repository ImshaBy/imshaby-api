package by.imsha.server.mapping.response.dto.parish;


import by.imsha.server.FieldNameGetter;

public class GetParishStateResponseDtoMapping implements FieldNameGetter {

    public static final String STATE_FIELD_QUALIFIER = "Статус";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (STATE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "state";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа на получение статуса парафии");
    }
}