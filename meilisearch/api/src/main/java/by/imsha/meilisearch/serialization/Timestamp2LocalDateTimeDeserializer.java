package by.imsha.meilisearch.serialization;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Timestamp2LocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

    //TODO всюду используется зона +3 (нужно хорошо всё обдумать и отрефакторить)
    private static final ZoneOffset BEL_ZONE_OFFSET = ZoneOffset.ofHours(3);

    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)  {
        return LocalDateTime.ofEpochSecond(jsonParser.getLongValue(), 0, BEL_ZONE_OFFSET);
    }
}
