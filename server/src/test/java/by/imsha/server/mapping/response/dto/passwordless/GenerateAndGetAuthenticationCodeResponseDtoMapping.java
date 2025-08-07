package by.imsha.server.mapping.response.dto.passwordless;


import by.imsha.server.FieldNameGetter;

public class GenerateAndGetAuthenticationCodeResponseDtoMapping implements FieldNameGetter {

    public static final String AUTHENTICATION_CODE_FIELD_QUALIFIER = "код аутентификации";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (AUTHENTICATION_CODE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "code";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа генерации и получения кода аутентификации");
    }
}