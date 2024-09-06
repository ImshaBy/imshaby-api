package by.imsha.meilisearch.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalTime;

public class Seconds2LocalTimeDeserializer extends JsonDeserializer<LocalTime> {

    @Override
    public LocalTime deserialize(final JsonParser jsonParser, final DeserializationContext ctxt) throws IOException {
        return LocalTime.ofSecondOfDay(jsonParser.getIntValue());
    }
}
