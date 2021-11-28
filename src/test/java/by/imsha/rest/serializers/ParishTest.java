package by.imsha.rest.serializers;

import by.imsha.domain.EntityWebhook;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.Ping;
import by.imsha.domain.dto.EntityWebHookType;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
