package by.imsha.server.mapping.request.dto.parish;

import by.imsha.server.FieldValueSetter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UpdateParishStateDtoMapping implements FieldValueSetter {

    public static final String STATE_FIELD_QUALIFIER = "Статус";
    private String state;

    @Override
    public ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue) {
        if (fieldName.equals(STATE_FIELD_QUALIFIER)) {
            return jsonNode.put("state", (String) fieldValue);
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldName + "' в модели обновления статуса парафии");
    }
}