package by.imsha.meilisearch.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Timestamp2LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    //TODO всюду используется зона +3 (нужно хорошо всё обдумать и отрефакторить)
    private static final ZoneOffset BEL_ZONE_OFFSET = ZoneOffset.ofHours(3);

    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return LocalDateTime.ofEpochSecond(jsonParser.getLongValue(), 0, BEL_ZONE_OFFSET);
    }
}
