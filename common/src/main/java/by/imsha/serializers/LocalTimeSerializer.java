package by.imsha.serializers;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
/**
 * Used to serialize java.time.LocalTime, which is not a common JSON
 * type, so we have to create a custom serialize method;.
 *
 * @author Andrei Misan
 */
@Component
public class LocalTimeSerializer extends ValueSerializer<LocalTime> {


    private String timeFormat = "HH:mm";

    @Override
    public void serialize(LocalTime time, JsonGenerator gen, SerializationContext provider)
            {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
        gen.writeString(time.format(formatter));
    }
}
