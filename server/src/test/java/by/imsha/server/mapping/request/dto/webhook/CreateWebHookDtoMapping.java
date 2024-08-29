package by.imsha.server.mapping.request.dto.webhook;

import by.imsha.server.FieldValueSetter;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CreateWebHookDtoMapping implements FieldValueSetter {

    public static final String KEY_FIELD_QUALIFIER = "Ключ";
    public static final String URL_FIELD_QUALIFIER = "Url";

    private String key;
    private String url;

    @Override
    public ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue) {
        if (fieldName.equals(KEY_FIELD_QUALIFIER)) {
            return jsonNode.put("key", (String) fieldValue);
        }
        if (fieldName.equals(URL_FIELD_QUALIFIER)) {
            return jsonNode.put("url", (String) fieldValue);
        }
        throw new IllegalArgumentException("Неизвестное поле '" + fieldName + "' в модели создания WebHook");
    }
}
