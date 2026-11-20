package by.imsha.meilisearch.serialization;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.io.IOException;
import java.time.LocalTime;

public class Seconds2LocalTimeDeserializer extends ValueDeserializer<LocalTime> {

    @Override
    public LocalTime deserialize(final JsonParser jsonParser, final DeserializationContext ctxt)  {
        return LocalTime.ofSecondOfDay(jsonParser.getIntValue());
    }
}
