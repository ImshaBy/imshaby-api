package by.imsha.service;

import by.imsha.Application;
import by.imsha.domain.City;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.meilisearch.model.SearchResultItem;
import by.imsha.meilisearch.reader.DefaultMeilisearchReader;
import by.imsha.meilisearch.reader.MassSearchFilter;
import by.imsha.repository.CityRepository;
import by.imsha.repository.ParishRepository;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Testcontainers
@SpringBootTest(classes = {Application.class, MonthlyMassIndexIT.FixedClockConfiguration.class})
class MonthlyMassIndexIT {
    private static final String MEILI_KEY = "monthly-index-test-master-key";
    private static final String INDEX_UID = "monthly-mass-index";

    @Container
    static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
    @Container
    static final GenericContainer<?> MEILI = new GenericContainer<>("getmeili/meilisearch:v1.12.1")
            .withExposedPorts(7700).withEnv("MEILI_MASTER_KEY", MEILI_KEY);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017)
                + "/imshaby_monthly_index");
        registry.add("meilisearch.writer.host-url", MonthlyMassIndexIT::meiliUrl);
        registry.add("meilisearch.writer.api-key", () -> MEILI_KEY);
        registry.add("meilisearch.writer.index-uid", () -> INDEX_UID);
        registry.add("meilisearch.reader.host-url", MonthlyMassIndexIT::meiliUrl);
        registry.add("meilisearch.reader.api-key", () -> MEILI_KEY);
        registry.add("meilisearch.reader.index-uid", () -> INDEX_UID);
    }

    @Autowired CityRepository cities;
    @Autowired ParishRepository parishes;
    @Autowired MongoTemplate mongo;
    @Autowired MassIndexService indexService;
    @Autowired MassService massService;
    @Autowired ObjectMapper json;

    @TestBean(methodName = "isolatedCacheManager", enforceOverride = true)
    CacheManager cacheManager;

    static CacheManager isolatedCacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Test
    void indexesMonthlyOccurrencesWithTheirDurationAndSkipsCancelledOccurrencesAndSeries() {
        City city = cities.save(City.builder().name("Monthly city").key("monthly-city").build());
        Parish parish = new Parish();
        parish.setName("Monthly parish");
        parish.setKey("monthly-parish");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);

        mongo.getCollection(mongo.getCollectionName(Mass.class)).insertMany(List.of(
                monthlyMass("active-series", city, parish, false, List.of()),
                monthlyMass("cancelled-occurrence", city, parish, false, List.of(Instant.parse("2026-11-13T08:45:00Z"))),
                monthlyMass("deleted-series", city, parish, true, List.of())));

        indexService.updateIndex();

        var reader = new DefaultMeilisearchReader(new Client(new Config(meiliUrl(), MEILI_KEY)), INDEX_UID, json);
        var hits = reader.searchAllMasses(MassSearchFilter.builder()
                .cityId(city.getId())
                .dateTimeFrom(OffsetDateTime.parse("2026-11-13T00:00:00+03:00"))
                .dateTimeTo(OffsetDateTime.parse("2026-11-21T23:59:59+03:00"))
                .build()).hits();
        assertThat(hits).extracting(SearchResultItem::massId, SearchResultItem::dateTime, SearchResultItem::duration)
                .containsExactly(tuple("active-series", LocalDateTime.of(2026, 11, 13, 11, 45), 2700));
    }

    @Test
    void indexesWeeklySeriesAfterItsFirstCancellation() {
        City city = cities.save(City.builder().name("Legacy city").key("legacy-city").build());
        Parish parish = new Parish();
        parish.setName("Legacy parish");
        parish.setKey("legacy-parish");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);

        mongo.getCollection(mongo.getCollectionName(Mass.class)).insertOne(new Document("_id", "legacy-weekly")
                .append("cityId", city.getId()).append("parishId", parish.getId())
                .append("langCode", "be")
                .append("calendar", new Document("dtstart", java.util.Date.from(Instant.parse("2026-11-13T05:45:00Z")))
                        .append("rrule", "FREQ=WEEKLY;BYDAY=FR,SU"))
                .append("duration", 3600L).append("deleted", false));

        massService.cancelOccurrence("legacy-weekly", Instant.parse("2026-11-13T05:45:00Z"));
        var legacy = mongo.findById("legacy-weekly", Mass.class);
        assertThat(legacy.getCalendar().dtstart()).isEqualTo(Instant.parse("2026-11-13T05:45:00Z"));
        assertThat(legacy.getCalendar().exdates()).containsExactly(Instant.parse("2026-11-13T05:45:00Z"));
        indexService.updateIndex();

        var reader = new DefaultMeilisearchReader(new Client(new Config(meiliUrl(), MEILI_KEY)), INDEX_UID, json);
        var hits = reader.searchAllMasses(MassSearchFilter.builder()
                .cityId(city.getId())
                .dateTimeFrom(OffsetDateTime.parse("2026-11-13T00:00:00+03:00"))
                .dateTimeTo(OffsetDateTime.parse("2026-11-21T23:59:59+03:00"))
                .build()).hits();
        assertThat(hits).extracting(SearchResultItem::massId, SearchResultItem::dateTime, SearchResultItem::duration)
                .containsExactlyInAnyOrder(tuple("legacy-weekly", LocalDateTime.of(2026, 11, 15, 8, 45), 3600),
                        tuple("legacy-weekly", LocalDateTime.of(2026, 11, 20, 8, 45), 3600));
    }

    private static Document monthlyMass(String id, City city, Parish parish,
                                        boolean deleted, List<Instant> exdates) {
        return new Document("_id", id)
                .append("cityId", city.getId())
                .append("parishId", parish.getId())
                .append("langCode", "be")
                .append("duration", 2700L)
                .append("deleted", deleted)
                .append("calendar", new Document("dtstart", java.util.Date.from(Instant.parse("2026-10-09T08:45:00Z")))
                        .append("rrule", "FREQ=MONTHLY;BYDAY=2FR")
                        .append("exdates", exdates.stream().map(java.util.Date::from).toList()));
    }

    private static String meiliUrl() {
        return "http://" + MEILI.getHost() + ":" + MEILI.getMappedPort(7700);
    }

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-14T09:00:00Z"), ZoneId.of("Europe/Minsk"));
        }
    }
}
