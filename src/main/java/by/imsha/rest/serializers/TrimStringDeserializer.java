package by.imsha.rest.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import java.io.IOException;

/**
 * Десериализатор для строковых значений, убирающий пробелы в начале и в конце строки
 */
public class TrimStringDeserializer extends StringDeserializer {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        final String value = super.deserialize(p, ctxt);
        return value == null ? null : value.trim();
    }
}
