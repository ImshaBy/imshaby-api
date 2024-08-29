package by.imsha.server.mapping.request.dto;

import by.imsha.server.FieldValueSetter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UpdateCityLocalizationDtoMapping implements FieldValueSetter {

    public static final String NAME_FIELD_QUALIFIER = "Наименование";
    private String name;

    @Override
    public ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue) {
        if (fieldName.equals(NAME_FIELD_QUALIFIER)) {
            return jsonNode.put("name", (String) fieldValue);
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldName + "' в модели обновления локализации города");
    }
}
