package by.imsha.server.mapping.request.dto.passwordless;

import by.imsha.server.FieldValueSetter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GenerateAndGetAuthenticationCodeRequestDtoMapping implements FieldValueSetter {

    public static final String EMAIL_FIELD_QUALIFIER = "e-mail";
    private String email;

    @Override
    public ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue) {
        if (fieldName.equals(EMAIL_FIELD_QUALIFIER)) {
            return jsonNode.put("email", (String) fieldValue);
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldName + "' в модели запроса генерации и получения кода аутентификации");
    }
}
