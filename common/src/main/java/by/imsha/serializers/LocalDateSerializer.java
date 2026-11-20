package by.imsha.serializers;


import by.imsha.utils.Constants;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Used to serialize java.time.LocalTime, which is not a common JSON
 * type, so we have to create a custom serialize method;.
 *
 * @author Andrei Misan
 */
@Component
public class LocalDateSerializer extends ValueSerializer<LocalDate> {


    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);

    @Override
    public void serialize(LocalDate date, JsonGenerator gen, SerializationContext provider)
            {
        gen.writeString(date.format(formatter));
    }


}
