package by.imsha.server.rest.serializers;

import by.imsha.domain.Parish;
import by.imsha.domain.dto.MassParishInfo;
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
        LocalDateTime lastConfirmRelevance = LocalDateTime.parse("2018-06-23T00:27:16", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        parish.setLastConfirmRelevance(lastConfirmRelevance);
        assertThat(parish.isNeedUpdate(), equalTo(Boolean.TRUE));
    }

    @Test
    public void testNeedUpdateIfLastConfirmRelevanceIsNull(){
        Parish parish = new Parish();
        parish.setUpdatePeriodInDays(14);
        parish.setLastConfirmRelevance(null);
        assertThat(parish.isNeedUpdate(), equalTo(Boolean.TRUE));
    }

    @Test
    public void testMassParishInfoNeedUpdate(){
        MassParishInfo massParishInfo = new MassParishInfo();
        massParishInfo.setUpdatePeriodInDays(14);
        LocalDateTime lastConfirmRelevance = LocalDateTime.parse("2018-06-23T00:27:16", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        massParishInfo.setLastConfirmRelevance(lastConfirmRelevance);
        assertThat(massParishInfo.isNeedUpdate(), equalTo(Boolean.TRUE));
    }

    @Test
    public void testMassParishInfoNeedUpdateWithLastModifiedDate(){
        MassParishInfo massParishInfo = new MassParishInfo();
        massParishInfo.setUpdatePeriodInDays(14);
        massParishInfo.setLastConfirmRelevance(null);
        massParishInfo.setLastModifiedDate(LocalDateTime.parse("2018-06-23T00:27:16", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        assertThat(massParishInfo.isNeedUpdate(), equalTo(Boolean.TRUE));
    }

}
