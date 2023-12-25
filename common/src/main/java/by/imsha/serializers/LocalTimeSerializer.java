package by.imsha.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
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
public class LocalTimeSerializer extends JsonSerializer<LocalTime> {


    private String timeFormat = "HH:mm";

    @Override
    public void serialize(LocalTime time, JsonGenerator gen, SerializerProvider provider)
            throws IOException{
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
        gen.writeString(time.format(formatter));
    }
}
