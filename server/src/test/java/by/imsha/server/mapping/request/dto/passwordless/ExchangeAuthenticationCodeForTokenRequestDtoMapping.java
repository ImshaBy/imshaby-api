package by.imsha.server.mapping.request.dto.passwordless;

import by.imsha.server.FieldValueSetter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ExchangeAuthenticationCodeForTokenRequestDtoMapping implements FieldValueSetter {

    public static final String AUTHENTICATION_CODE_KEY = "код аутентификации";
    private String code;

    @Override
    public ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue) {
        if (fieldName.equals(AUTHENTICATION_CODE_KEY)) {
            return jsonNode.put("code", (String) fieldValue);
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldName + "' в модели запроса обмена кода аутентификации на токен");
    }
}
