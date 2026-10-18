package by.imsha.meilisearch.serialization;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.io.IOException;
import java.time.LocalDate;

public class Timestamp2LocalDateDeserializer extends ValueDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)  {
        return LocalDate.ofEpochDay(jsonParser.getLongValue());
    }
}