package by.imsha.listeners;

import by.imsha.domain.EntityWebhook;
import by.imsha.domain.Mass;
import by.imsha.domain.Ping;
import by.imsha.domain.dto.MassInfo;
import by.imsha.domain.dto.mapper.MassInfoMapper;
import by.imsha.service.CityService;
import by.imsha.service.EntityWebhookService;
import by.imsha.utils.ServiceUtils;
import com.google.common.collect.Maps;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveCallback;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.function.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class WebHookListener implements AfterSaveCallback<Mass> {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EntityWebhookService webhookService;

    @Value("${spring.security.oauth2.client.hook.token}")
    private String hookToken;

    private final WebClient webClient = WebClient.builder()
            .build();

    private Map<String, LocalDateTime> parishEventTimestamps = Maps.newConcurrentMap();


    @Override
    public Mass onAfterSave(Mass mass, Document document, String s) {
        String parishId = mass.getParishId();

        LocalDateTime parishLastModifiedTimeEvent = parishEventTimestamps.get(parishId);
        LocalDateTime massLastModifiedTime = mass.getLastModifiedDate();
        boolean needToFireEvent = parishLastModifiedTimeEvent != null
                ? ServiceUtils.hourDiff(parishLastModifiedTimeEvent, massLastModifiedTime) > 1
                : true;
        log.warn("parishLastModifiedTimeEvent = " + parishLastModifiedTimeEvent);
        log.warn("massLastModifiedTime = " + massLastModifiedTime);
        log.warn("needToFireEvent = " + needToFireEvent);
        if(needToFireEvent){
            List<EntityWebhook> citiesHooks = webhookService.retrieveCityHooks(mass);
            List<EntityWebhook> parishHooks = webhookService.retrieveParishHooks(mass);

            Flux.fromIterable(citiesHooks)
                    .parallel()
                    .runOn(Schedulers.boundedElastic())
                    .flatMap(it -> hitWebHook(it.getUrl(), mass))
                    .subscribe(out -> System.out.format("Subscriber cities -> %s, thread name : %s \n", out, Thread.currentThread().getName()));

            Flux.fromIterable(parishHooks)
                    .parallel()
                    .runOn(Schedulers.parallel())
                    .flatMap(it -> hitWebHook(it.getUrl(), mass))
                    .subscribe(out -> System.out.format("Subscriber parish -> %s, thread name : %s \n" ,out, Thread.currentThread().getName()));

            parishEventTimestamps.put(parishId, massLastModifiedTime);
        }

        return mass;
    }



    private Mono<String> hitWebHook(String url, Mass mass) {
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + hookToken)
                .body(Mono.just(MassInfoMapper.MAPPER.toMassInfo(mass)), MassInfo.class)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just("Error " + e.getMessage()));
    }
}
