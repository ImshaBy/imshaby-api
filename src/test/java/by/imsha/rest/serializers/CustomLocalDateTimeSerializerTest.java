package by.imsha.rest.serializers;

import by.imsha.utils.ServiceUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Alena Misan
 */

public class CustomLocalDateTimeSerializerTest {

    private final String pattern = "dd-MM-yyyy HH:mm:ss";
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
    private CustomLocalDateTimeSerializer customTimeSerializer;

    @BeforeAll
    public void setUp(){
        customTimeSerializer = new CustomLocalDateTimeSerializer();
    }

    @Test
    public void shouldTransformLondonTime_212505_To_MinskTime_002505(){
        String minskTimeString = "25-01-2018 00:25:05";
        String londonTimeString = "24-01-2018 21:25:05";

        LocalDateTime londonLocalDateTime = LocalDateTime.parse(londonTimeString, dateTimeFormatter);

        ZonedDateTime minskTime = ServiceUtils.localDateTimeToZoneDateTime(londonLocalDateTime, ZoneId.of("Europe/London"), ZoneId.of("Europe/Minsk"));
//        System.out.println("minskTime = " + minskTime);
        assertThat(ZoneId.of("Europe/Minsk"), is(equalTo(minskTime.getZone())));
    }

}
