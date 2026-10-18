package by.imsha.meilisearch.serialization;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import java.io.IOException;
import java.time.LocalDate;

public class LocalDate2TimestampSerializer extends ValueSerializer<LocalDate> {

    @Override
    public void serialize(LocalDate localDate, JsonGenerator jsonGenerator, SerializationContext serializerProvider)  {
        jsonGenerator.writeNumber(localDate.toEpochDay());
    }
}