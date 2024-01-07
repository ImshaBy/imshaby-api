package by.imsha.server.utils;

import by.imsha.utils.ServiceUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Alena Misan
 */

public class ServiceUtilsTest {

    @Test
    public void transformTimestampToDate_03122017(){
        LocalDateTime result =  ServiceUtils.timestampToLocalDate(1512399600, ZoneId.of("Europe/Minsk"));
        assertThat(result.getDayOfMonth(), equalTo(4));
    }

    @Test
    public void transformTimestampToDate_03032019(){
//        LocalDateTime result =  ServiceUtils.timestampToLocalDate(Long.parseLong("1551639600000"), ZoneId.of("Europe/Minsk"));
        LocalDateTime result =  ServiceUtils.timestampToLocalDate(Long.parseLong("1551610800"), ZoneId.of("Europe/Minsk"));
        assertThat(result.getDayOfMonth(), equalTo(3));
    }

    @Test
    public void testOffsetLimitPositive(){
        int[] result = ServiceUtils.calculateOffsetAndLimit(0,20);
        assertThat(result[0], equalTo(0));
        assertThat(result[1], equalTo(20));
    }


    @Test
    public void testOffsetLimitPaging(){
        int[] result = ServiceUtils.calculateOffsetAndLimit(10,20);
        assertThat(result[0], equalTo(0));
        assertThat(result[1], equalTo(20));
    }

    @Test
    public void testOffsetLimitNegative(){
        int[] result = ServiceUtils.calculateOffsetAndLimit(3,5);
        assertThat(result[0], equalTo(0));
        assertThat(result[1], equalTo(5));
    }


    @Test
    public void checkDateToTimestamp(){
        long timestamp = ServiceUtils.dateToUTCTimestamp("03-12-2017");
        assertThat(timestamp, equalTo(1512259200L));
    }

    @Test
    public void checkDateTimeToTimestamp(){
        long timestamp = ServiceUtils.dateTimeToUTCTimestamp("03-07-2018 15:35");
        assertThat(timestamp, equalTo(1530632100L));
    }

}
