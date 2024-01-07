package by.imsha.server.rest.serializers;

import by.imsha.domain.Parish;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Alena Misan
 */
public class ParishTest {

    @Test
    public void testNeedUpdate(){
        Parish parish = new Parish();
        parish.setUpdatePeriodInDays(14);
        LocalDateTime lastModifiedDate = LocalDateTime.parse("2018-06-23T00:27:16", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        parish.setLastModifiedDate(lastModifiedDate);
        assertThat(parish.isNeedUpdate(), equalTo(Boolean.TRUE));
    }

    @Test
    public void testNeedUpdateIfLastModifiedDateIsNull(){
        Parish parish = new Parish();
        parish.setUpdatePeriodInDays(14);
        parish.setLastModifiedDate(null);
        assertThat(parish.isNeedUpdate(), equalTo(Boolean.TRUE));
    }

}
