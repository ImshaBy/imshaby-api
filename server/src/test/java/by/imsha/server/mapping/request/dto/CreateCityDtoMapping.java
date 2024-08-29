package by.imsha.server.mapping.request.dto;

import by.imsha.server.FieldValueSetter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CreateCityDtoMapping implements FieldValueSetter {

    public static final String NAME_FIELD_QUALIFIER = "Наименование";
    public static final String KEY_FIELD_QUALIFIER = "Ключ";
    private String name;
    private String key;

    @Override
    public ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue) {
        if (fieldName.equals(NAME_FIELD_QUALIFIER)) {
            return jsonNode.put("name", (String) fieldValue);
        }
        if (fieldName.equals(KEY_FIELD_QUALIFIER)) {
            return jsonNode.put("key", (String) fieldValue);
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldName + "' в модели создания города");
    }
}
