package by.imsha.meilisearch.serialization;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import java.io.IOException;
import java.time.LocalTime;

public class LocalTime2SecondsSerializer extends ValueSerializer<LocalTime> {

    @Override
    public void serialize(final LocalTime localTime, final JsonGenerator gen, final SerializationContext serializers)  {
        gen.writeNumber(localTime.toSecondOfDay());
    }
}
