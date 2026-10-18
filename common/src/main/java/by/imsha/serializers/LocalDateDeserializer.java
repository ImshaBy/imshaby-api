package by.imsha.serializers;


import by.imsha.utils.Constants;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
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
public class LocalDateDeserializer extends ValueDeserializer<LocalDate> {


    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)  {
        return LocalDate.parse(jsonParser.getValueAsString(), formatter);
    }


}
