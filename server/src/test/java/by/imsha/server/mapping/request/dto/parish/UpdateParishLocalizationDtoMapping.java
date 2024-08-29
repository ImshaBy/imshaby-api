package by.imsha.server.mapping.request.dto.parish;

import by.imsha.server.FieldValueSetter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UpdateParishLocalizationDtoMapping implements FieldValueSetter {

    public static final String NAME_FIELD_QUALIFIER = "Наименование";
    public static final String SHORT_NAME_FIELD_QUALIFIER = "Краткое наименование";
    public static final String ADDRESS_FIELD_QUALIFIER = "Адрес";
    private String name;
    private String shortName;
    private String address;

    @Override
    public ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue) {
        if (fieldName.equals(NAME_FIELD_QUALIFIER)) {
            return jsonNode.put("name", (String) fieldValue);
        }
        if (fieldName.equals(SHORT_NAME_FIELD_QUALIFIER)) {
            return jsonNode.put("shortName", (String) fieldValue);
        }
        if (fieldName.equals(ADDRESS_FIELD_QUALIFIER)) {
            return jsonNode.put("address", (String) fieldValue);
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldName + "' в модели обновления локализации парафии");
    }
}