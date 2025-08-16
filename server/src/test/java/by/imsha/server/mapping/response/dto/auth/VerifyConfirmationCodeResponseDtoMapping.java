package by.imsha.server.mapping.response.dto.auth;


import by.imsha.server.FieldNameGetter;

public class VerifyConfirmationCodeResponseDtoMapping implements FieldNameGetter {

    public static final String SUCCESS_FIELD_QUALIFIER = "верифицирован";

    @Override
    public String getFieldName(String fieldQualifier) {
        if (SUCCESS_FIELD_QUALIFIER.equals(fieldQualifier)) {
            return "valid";
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldQualifier + "' в модели ответа проверки кода подтверждения");
    }
}