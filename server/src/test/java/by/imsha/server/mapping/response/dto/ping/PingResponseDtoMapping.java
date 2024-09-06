package by.imsha.server.mapping.response.dto.ping;

import by.imsha.server.FieldNameGetter;

public class PingResponseDtoMapping implements FieldNameGetter {

    public static final String NAME_FIELD_QUALIFIER = "Наименование";
    @Override
    public String getFieldName(String fieldQualifier) {
        if (NAME_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "name";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа на получение ping");
    }
}