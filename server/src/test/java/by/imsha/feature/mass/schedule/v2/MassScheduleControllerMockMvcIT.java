package by.imsha.feature.mass.schedule.v2;

import by.imsha.Application;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.meilisearch.reader.MeilisearchReader;
import by.imsha.repository.MassRepository;
import by.imsha.repository.ParishRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "qa"})
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MassScheduleControllerMockMvcIT {
    private static final String URL = "/api/v2/masses";
    private static final String API_KEY = "TEST_API_KEY";
    private static final String PARISH = "monthly-test-parish";
    private static final String CITY = "city-1";

    @Container
    static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
    static final WireMockServer IDENTITY = new WireMockServer(options().dynamicPort());
    static {
        IDENTITY.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":"
                + MONGO.getMappedPort(27017) + "/imshaby_mass_schedule_v2");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
    }

    @AfterAll
    static void stopIdentity() {
        IDENTITY.stop();
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MongoTemplate mongo;
    @Autowired MassRepository masses;
    @Autowired ParishRepository parishes;
    @Autowired CacheManager caches;
    @MockitoBean MeilisearchReader meilisearchReader;

    @BeforeEach
    void reset() {
        masses.deleteAll();
        parishes.deleteAll();
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        IDENTITY.resetAll();

        Parish parish = new Parish();
        parish.setId(PARISH);
        parish.setKey(PARISH);
        parish.setName("St. Mary");
        parish.setCityId(CITY);
        parish.setState(Parish.State.APPROVED);
        parishes.save(parish);
    }

    @Test
    void roundTripsSingleAndBoundedWeeklySchedules() throws Exception {
        JsonNode single = create("roundTripsSingleAndBoundedWeeklySchedules", "single");
        assertThat(stored(single.path("id").asText()).get("calendar", Document.class))
                .containsEntry("dtstart", Date.from(Instant.parse("2026-09-08T04:15:00Z")))
                .containsEntry("rrule", "FREQ=DAILY;UNTIL=20260908T041500Z")
                .containsEntry("exdates", List.of());
        JsonNode weekly = create("roundTripsSingleAndBoundedWeeklySchedules", "weekly");
        assertThat(stored(weekly.path("id").asText()).get("calendar", Document.class))
                .containsEntry("dtstart", Date.from(Instant.parse("2026-09-08T05:00:00Z")))
                .containsEntry("rrule", "FREQ=WEEKLY;BYDAY=TH,TU;UNTIL=20260910T050000Z");
    }

    @Test
    void cancelsOnlyAnOccurrenceThenCancelsTheSeries() throws Exception {
        JsonNode response = create("cancelsOnlyAnOccurrenceThenCancelsTheSeries", "monthly");
        String id = response.path("id").asText();
        assertThat(stored(id).get("calendar", Document.class))
                .containsEntry("dtstart", Date.from(Instant.parse("2026-09-11T06:15:00Z")))
                .containsEntry("rrule", "FREQ=MONTHLY;BYDAY=2FR");

        mvc.perform(delete(URL + "/{id}/occurrences/{start}", id, "2026-10-02T06:15:00Z")
                        .header("X-Api-Key", API_KEY))
                .andExpect(status().isNotFound());
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(delete(URL + "/{id}/occurrences/{start}", id, "2026-09-11T06:15:00Z").header("X-Api-Key", API_KEY))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));
        }
        assertThat(stored(id).get("calendar", Document.class))
                .containsEntry("exdates", List.of(Date.from(Instant.parse("2026-09-11T06:15:00Z"))));
        assertThat(read(id).path("exdates")).hasSize(1);
        assertThat(instant(read(id).path("exdates").get(0))).isEqualTo(Instant.parse("2026-09-11T06:15:00Z"));

        mvc.perform(delete(URL + "/{id}", id).header("X-Api-Key", API_KEY))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(stored(id)).containsEntry("deleted", true);
        assertThat(stored(id).get("calendar", Document.class))
                .containsEntry("exdates", List.of(Date.from(Instant.parse("2026-09-11T06:15:00Z"))));
        mvc.perform(get(URL + "/{id}", id).header("X-Api-Key", API_KEY))
                .andExpect(status().isNotFound());
        mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("cancelsOnlyAnOccurrenceThenCancelsTheSeries", "monthly-request")))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsMalformedRulesAndStartsOutsideTheRule() throws Exception {
        String request = fixture("cancelsOnlyAnOccurrenceThenCancelsTheSeries", "monthly-request");
        for (String invalidRule : List.of("FREQ=WEEKLY;BYDAY=TH",
                "FREQ=MONTHLY;BYDAY=2FR;UNTIL=20260904T061500Z", "FREQ=WEEKLY;BYDAY")) {
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(request.replace("FREQ=MONTHLY;BYDAY=2FR", invalidRule)))
                    .andExpect(status().isBadRequest());
        }
        for (String invalid : List.of(request.replace("monthly-test-parish", " "),
                request.replace("\"cityId\":\"city-1\",", ""),
                request.replace("\"dtstart\":\"2026-09-11T06:15:00Z\",", ""),
                request.replace("\"langCode\":\"be\"", "\"langCode\":\"\""),
                request.replace("\"duration\":4500", "\"duration\":0"))) {
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(invalid))
                    .andExpect(status().isBadRequest());
        }
        assertThat(masses.count()).isZero();
    }

    @Test
    void cancelledOccurrenceNoLongerConflicts() throws Exception {
        String id = create("cancelsOnlyAnOccurrenceThenCancelsTheSeries", "monthly").path("id").asText();
        String start = "2026-09-11T06:15:00Z";
        String once = "FREQ=DAILY;UNTIL=20260911T061500Z";
        assertConflict(start, once, id);

        mvc.perform(delete(URL + "/{id}/occurrences/{start}", id, start)
                        .header("X-Api-Key", API_KEY)).andExpect(status().isNoContent());
        mvc.perform(postSchedule(start, once)).andExpect(status().isCreated());
    }

    @Test
    void earlierBoundedScheduleChecksThroughItsLastOccurrenceEvenAfterOneYear() throws Exception {
        String id = body(mvc.perform(postSchedule("2025-01-01T05:00:00Z",
                        "FREQ=WEEKLY;BYDAY=WE;UNTIL=20260107T050000Z"))
                .andExpect(status().isCreated()).andReturn()).path("id").asText();
        assertConflict("2026-01-07T05:00:00Z", "FREQ=DAILY;UNTIL=20260107T050000Z", id);
        mvc.perform(postSchedule("2026-01-14T05:00:00Z", "FREQ=DAILY;UNTIL=20260114T050000Z"))
                .andExpect(status().isCreated());
    }

    @Test
    void concurrentCreatesPersistEachSuccessfulRequest() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = List.of("FREQ=WEEKLY;BYDAY=FR", "FREQ=MONTHLY;BYDAY=2FR").stream()
                    .map(rule -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Concurrent requests did not start");
                        }
                        String dtstart = rule.contains("WEEKLY")
                                ? "2026-09-11T06:15:00Z" : "2026-10-09T06:15:00Z";
                        return mvc.perform(postSchedule(dtstart, rule))
                                .andReturn().getResponse().getStatus();
                    })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var statuses = List.of(results.get(0).get(15, TimeUnit.SECONDS), results.get(1).get(15, TimeUnit.SECONDS));
            assertThat(statuses).contains(201).allMatch(status -> status == 201 || status == 400);
            assertThat(masses.count()).isEqualTo(statuses.stream().filter(status -> status == 201).count());
        }
    }

    private void assertConflict(String start, String rule, String conflictingId) throws Exception {
        JsonNode response = body(mvc.perform(postSchedule(start, rule))
                .andExpect(status().isBadRequest()).andReturn());
        var error = response.path("errors").get(0);
        assertThat(error.path("code").asText()).isEqualTo("MASS.011");
        var duplicate = error.path("payload").path("duplicateMass");
        assertThat(duplicate.path("id").asText()).isEqualTo(conflictingId);
        assertThat(duplicate.path("calendar").isObject()).isTrue();
    }

    private MockHttpServletRequestBuilder postSchedule(
            String dtstart, String rule) throws Exception {
        String request = fixture("cancelsOnlyAnOccurrenceThenCancelsTheSeries", "monthly-request")
                .replace("2026-09-11T06:15:00Z", dtstart).replace("FREQ=MONTHLY;BYDAY=2FR", rule);
        return post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON).content(request);
    }

    private JsonNode create(String scenario, String variant) throws Exception {
        var response = mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture(scenario, variant + "-request")))
                .andExpect(status().isCreated()).andReturn();
        JsonNode result = body(response);
        String id = result.path("id").asText();
        assertThat(id).isNotEmpty();
        assertThat(response.getResponse().getHeader("Location")).isNull();
        JsonNode expected = json.readTree(fixture(scenario, variant + "-response")
                .replace("${id}", id)
                .replace("${serializedStart}", result.path("dtstart").asText()));
        JsonNode request = json.readTree(fixture(scenario, variant + "-request"));
        assertThat(instant(result.path("dtstart"))).isEqualTo(instant(request.path("dtstart")));
        assertThat(result).isEqualTo(expected);
        assertThat(read(id)).isEqualTo(expected);
        return result;
    }

    private JsonNode read(String id) throws Exception {
        return body(mvc.perform(get(URL + "/{id}", id).header("X-Api-Key", API_KEY))
                .andExpect(status().isOk()).andReturn());
    }

    private Document stored(String id) {
        return mongo.getCollection(mongo.getCollectionName(Mass.class))
                .find(new Document("_id", new ObjectId(id))).first();
    }


    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static Instant instant(JsonNode value) {
        return OffsetDateTime.parse(value.asText()).toInstant();
    }

    private static String fixture(String scenario, String name) throws Exception {
        return new ClassPathResource("feature/mass/schedule/v2/" + scenario + "/" + name + ".json")
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
