package by.imsha.meilisearch.serialization;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class LocalDateTime2TimestampSerializer extends ValueSerializer<LocalDateTime> {

    //TODO всюду используется зона +3 (нужно хорошо всё обдумать и отрефакторить)
    private static final ZoneOffset BEL_ZONE_OFFSET = ZoneOffset.ofHours(3);

    @Override
    public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator, SerializationContext serializerProvider)  {
        jsonGenerator.writeNumber(localDateTime.toEpochSecond(BEL_ZONE_OFFSET));
    }
}
