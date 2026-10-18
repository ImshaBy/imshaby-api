package by.imsha.serializers;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.jdk.StringDeserializer;

import java.io.IOException;

/**
 * Десериализатор для строковых значений, убирающий пробелы в начале и в конце строки
 */
public class TrimStringDeserializer extends StringDeserializer {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt)  {
        final String value = super.deserialize(p, ctxt);
        return value == null ? null : value.trim();
    }
}
