package by.imsha.server.mapping.response.dto.passwordless;


import by.imsha.server.FieldNameGetter;

public class ExchangeAuthenticationCodeForTokenResponseDtoMapping implements FieldNameGetter {

    public static final String AUTHENTICATION_CODE_FIELD_QUALIFIER = "токен";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (AUTHENTICATION_CODE_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "token";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа обмена кода аутентификации на токен");
    }
}