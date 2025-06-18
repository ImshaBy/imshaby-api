package by.imsha.meilisearch.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class LocalDateTime2TimestampSerializer extends JsonSerializer<LocalDateTime> {

    //TODO всюду используется зона +3 (нужно хорошо всё обдумать и отрефакторить)
    private static final ZoneOffset BEL_ZONE_OFFSET = ZoneOffset.ofHours(3);

    @Override
    public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeNumber(localDateTime.toEpochSecond(BEL_ZONE_OFFSET));
    }
}
