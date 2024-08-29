package by.imsha.server.mapping.response.dto;

import by.imsha.server.FieldNameGetter;

public class CreateCityResponseDtoMapping implements FieldNameGetter {

    public static final String IDENTIFIER_FIELD_QUALIFIER = "Идентификатор";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (IDENTIFIER_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "id";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа на создание города");
    }
}
