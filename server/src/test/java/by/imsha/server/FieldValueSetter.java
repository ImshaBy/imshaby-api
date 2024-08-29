package by.imsha.server;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface FieldValueSetter {

    ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue);

    class Default implements FieldValueSetter {

        @Override
        public ObjectNode apply(ObjectNode jsonNode, String fieldName, Object fieldValue) {
            throw new IllegalArgumentException("Неизвестное поле '" + fieldName + "' в модели запроса");
        }
    }
}
