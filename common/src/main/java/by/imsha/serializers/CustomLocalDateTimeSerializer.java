package by.imsha.serializers;


import by.imsha.utils.ServiceUtils;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Used to serialize java.time.LocalTime, which is not a common JSON
 * type, so we have to create a custom serialize method;.
 *
 * @author Andrei Misan
 */
@Component
public class CustomLocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

    private String dateTimeFormat = "dd-MM-yyyy HH:mm:ss";
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat);


    @Override
    public void serialize(LocalDateTime date, JsonGenerator gen, SerializationContext provider)
            {
        ZonedDateTime timeString = ServiceUtils.localDateTimeToZoneDateTime(date, ZoneId.systemDefault(), ServiceUtils.BEL_ZONE_ID);
        gen.writeString(timeString.format(formatter));
    }


}
