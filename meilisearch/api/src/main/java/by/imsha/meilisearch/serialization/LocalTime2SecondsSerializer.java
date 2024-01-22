package by.imsha.meilisearch.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalTime;

public class LocalTime2SecondsSerializer extends JsonSerializer<LocalTime> {

    @Override
    public void serialize(final LocalTime localTime, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        gen.writeNumber(localTime.toSecondOfDay());
    }
}
