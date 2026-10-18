package by.imsha.service;

import by.imsha.Application;
import by.imsha.domain.City;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.meilisearch.reader.DefaultMeilisearchReader;
import by.imsha.meilisearch.reader.MassSearchFilter;
import by.imsha.repository.CityRepository;
import by.imsha.repository.MassRepository;
import by.imsha.repository.ParishRepository;
import by.imsha.utils.DateTimeProvider;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = Application.class)
class MassIndexPipelineIT {
    private static final String MEILI_KEY = "pipeline-test-master-key";
    @Container static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
    @Container static final GenericContainer<?> MEILI = new GenericContainer<>("getmeili/meilisearch:v1.12.1")
            .withExposedPorts(7700).withEnv("MEILI_MASTER_KEY", MEILI_KEY);

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_index_pipeline");
        registry.add("meilisearch.writer.host-url", () -> "http://" + MEILI.getHost() + ":" + MEILI.getMappedPort(7700));
        registry.add("meilisearch.writer.api-key", () -> MEILI_KEY);
        registry.add("meilisearch.writer.index-uid", () -> "mass-index-pipeline");
        registry.add("meilisearch.reader.host-url", () -> "http://" + MEILI.getHost() + ":" + MEILI.getMappedPort(7700));
        registry.add("meilisearch.reader.api-key", () -> MEILI_KEY);
        registry.add("meilisearch.reader.index-uid", () -> "mass-index-pipeline");
    }

    @Autowired CityRepository cities;
    @Autowired ParishRepository parishes;
    @Autowired MassRepository masses;
    @Autowired MassIndexService indexService;
    @Autowired DateTimeProvider dates;
    @Autowired ObjectMapper json;

    @Test void indexesOnlyApprovedMassAndReaderFindsIt() {
        City city = cities.save(City.builder().name("Pipeline city").key("pipeline-city").build());
        Parish parish = new Parish();
        parish.setName("Pipeline parish");
        parish.setKey("pipeline-parish");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass mass = new Mass();
        mass.setCityId(city.getId());
        mass.setParishId(parish.getId());
        mass.setLangCode("be");
        mass.setTime("08:00");
        mass.setDays(new int[]{1, 2, 3, 4, 5, 6, 7});
        mass = masses.save(mass);
        Parish pending = new Parish();
        pending.setName("Pipeline pending parish");
        pending.setKey("pipeline-pending-parish");
        pending.setCityId(city.getId());
        pending.setState(Parish.State.PENDING);
        pending = parishes.save(pending);
        Mass pendingMass = new Mass();
        pendingMass.setCityId(city.getId());
        pendingMass.setParishId(pending.getId());
        pendingMass.setLangCode("be");
        pendingMass.setTime("09:00");
        pendingMass.setDays(new int[]{1, 2, 3, 4, 5, 6, 7});
        pendingMass = masses.save(pendingMass);

        indexService.updateIndex();

        var reader = new DefaultMeilisearchReader(new Client(new Config(
                "http://" + MEILI.getHost() + ":" + MEILI.getMappedPort(7700), MEILI_KEY)),
                "mass-index-pipeline", json);
        LocalDate today = dates.today();
        var hits = reader.searchAllMasses(MassSearchFilter.builder()
                .cityId(city.getId())
                .dateTimeFrom(today.atStartOfDay().atOffset(ZoneOffset.ofHours(3)))
                .dateTimeTo(today.atTime(LocalTime.MAX).atOffset(ZoneOffset.ofHours(3)))
                .build()).hits();
        assertThat(hits).extracting(item -> item.massId()).contains(mass.getId())
                .doesNotContain(pendingMass.getId());
        assertThat(hits).extracting(item -> item.parish().id()).contains(parish.getId())
                .doesNotContain(pending.getId());
        assertThat(hits).extracting(item -> item.parish().state()).containsOnly("APPROVED");
    }
}
