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

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:3000").build();


    private Mono<Ping> hitWebHook(String url) {
        return webClient.get()
                .uri("ping1")
                .retrieve()
                .bodyToMono(Ping.class);
    }

    @Test
    public void testFlux(){

        List<EntityWebhook> citiesHooks = new ArrayList<>();
        citiesHooks.add(EntityWebhook.builder().type(EntityWebHookType.CITY.getType()).key("minsk").url("http://localhost/ping1").build());
        citiesHooks.add(EntityWebhook.builder().type(EntityWebHookType.CITY.getType()).key("minsk2").url("http://localhost/ping1").build());

       Flux.fromIterable(citiesHooks)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .flatMap(it -> hitWebHook(it.getUrl()))
                .ordered((u1, u2) -> u2.getName().hashCode() - u1.getName().hashCode())
               .blockLast();
//                .subscribe(value -> System.out.println("value = " + value));



//        System.out.println("ping = " + ping);
    }


    @Test
    public void testMono(){
        System.out.println("webClient = " + webClient);
        Mono<Ping> monoPing = webClient.get()
                .uri("ping1")
                .retrieve()
                .bodyToMono(Ping.class);

        Ping ping = monoPing.block();
        System.out.println("ping = " + ping);
    }

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
