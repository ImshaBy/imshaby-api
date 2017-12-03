package by.imsha.api.rest.serializers;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
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
public class CustomLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

    private String dateTimeFormat = "dd-MM-yyyy HH:mm:ss";

    @Override
    public void serialize(LocalDateTime date, JsonGenerator gen, SerializerProvider provider)
            throws IOException{
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat);
        String timeString = ZonedDateTime.of(date, ZoneId.of("Europe/Minsk")).format(formatter);
//        gen.writeString(date.format(formatter));
        gen.writeString(timeString);
    }
}