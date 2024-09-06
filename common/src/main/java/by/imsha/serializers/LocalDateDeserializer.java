package by.imsha.serializers;


import by.imsha.utils.Constants;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
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
public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {


    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        return LocalDate.parse(jsonParser.getValueAsString(), formatter);
    }


}
