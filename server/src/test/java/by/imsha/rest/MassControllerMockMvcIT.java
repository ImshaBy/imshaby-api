package by.imsha.rest;

import by.imsha.Application;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.utils.DateTimeProvider;
import by.imsha.repository.MassRepository;
import by.imsha.repository.ParishRepository;
import by.imsha.repository.CityRepository;
import by.imsha.meilisearch.reader.MeilisearchReader;
import by.imsha.meilisearch.reader.SearchResult;
import by.imsha.meilisearch.reader.MassSearchFilter;
import by.imsha.meilisearch.model.SearchResultItem;
import by.imsha.meilisearch.model.Geo;
import org.bson.Document;
import org.bson.types.ObjectId;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import jakarta.servlet.http.Cookie;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = Application.class)
@org.springframework.context.annotation.Import(FixedClockTestConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "qa"})
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MassControllerMockMvcIT {
    private static final String URL = "/api/mass";
    private static final String KEY = "TEST_API_KEY";
    @Container static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
    static final WireMockServer IDENTITY = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    static { IDENTITY.start(); }
    @AfterAll static void stopStub() { IDENTITY.stop(); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_mass_mockmvc");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
    }
    @Autowired MockMvc mvc;
    @MockitoSpyBean DateTimeProvider dateTimeProvider;
    @Autowired MassRepository masses;
    @Autowired MongoTemplate mongo;
    @Autowired ParishRepository parishes;
    @Autowired CityRepository cities;
    @Autowired CacheManager caches;
    @Autowired ObjectMapper json;
    private V1TestApi api;
    @MockitoBean MeilisearchReader meilisearchReader;
    private String directory;
    @BeforeEach void isolate(TestInfo info) {
        directory = "mass-mockmvc/" + info.getTestMethod().orElseThrow().getName() + "/";
        api = new V1TestApi(mvc, json);
        masses.deleteAll();
        parishes.deleteAll();
        cities.deleteAll();
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        IDENTITY.resetAll();
        when(dateTimeProvider.nowSystemDefaultZone()).thenReturn(LocalDateTime.now().withNano(0));
    }
    private String fixture(String name) throws Exception {
        return new ClassPathResource(directory + name).getContentAsString(StandardCharsets.UTF_8);
    }
    private String saved(String parishId, String startDate) throws Exception {
        return api.create(URL, Map.of(
                "cityId", "city-1",
                "parishId", parishId,
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1},
                "startDate", startDate));
    }

    private Document storedMass(String id) {
        return mongo.getCollection(mongo.getCollectionName(Mass.class))
                .find(new Document("_id", new ObjectId(id))).first();
    }

    private void assertCalendar(String id, String dtstart, String rrule) {
        assertThat(storedMass(id).get("calendar", Document.class))
                .containsEntry("dtstart", dtstart)
                .containsEntry("rrule", rrule);
    }

    private void assertReadMatchesCreation(String id, JsonNode created) throws Exception {
        MvcResult result = mvc.perform(get(URL + "/" + id).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(created);
    }
    private Mass savedLegacy(String parishId) {
        Mass mass = new Mass();
        mass.setCityId("city-1");
        mass.setParishId(parishId);
        mass.setLangCode("be");
        mass.setTime("08:00");
        mass.setDays(new int[]{1});
        return masses.save(mass);
    }
    private void createFilteredMass(String parishId, String time, String language, boolean online, boolean rorate)
            throws Exception {
        api.create(URL, Map.of("cityId", "city-1", "parishId", parishId, "langCode", language,
                "time", time, "days", List.of(1), "startDate", "06/24/2024", "online", online, "rorate", rorate));
    }

    private SearchResultItem indexedMass(String cityId, String parishId) {
        return new SearchResultItem("record-1", "mass-1", 60,
                LocalDateTime.of(2024, 6, 24, 8, 0),
                new by.imsha.meilisearch.model.Parish(parishId, true, "APPROVED"),
                "Morning mass", "be", false, false,
                new by.imsha.meilisearch.model.City(cityId), null,
                new Geo(53.9, 27.57), 120L);
    }
    private void assertJson(MvcResult result, String id) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", id)));
    }
    private String parishConfirmation(String parishId) throws Exception {
        return api.getEntity(V1TestApi.PARISHES, parishId).path("lastConfirmRelevance").asText();
    }

    private String checkedModified(JsonNode node) {
        String value = node.path("lastModifiedDate").asText();
        assertThat(value).isNotBlank();
        assertThat(LocalDateTime.parse(value, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))).isNotNull();
        return value;
    }
    private void assertValidationError(MvcResult result, String code, String field) throws Exception {
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.path("errors").size()).isEqualTo(1);
        assertThat(body.path("errors").get(0).path("code").asText()).isEqualTo(code);
        assertThat(body.path("errors").get(0).path("field").asText()).isEqualTo(field);
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${timestamp}", body.path("timestamp").asText())));
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void createPeriodicMass() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        String id = body.path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${id}", id)
                .replace("${lastModifiedDate}", checkedModified(body))));
        assertThat(api.getEntity(V1TestApi.MASSES, id).path("days")).isEqualTo(json.readTree("[1]"));
        assertThat(masses.findById(id).orElseThrow().getDays()).containsExactly(1);
        assertCalendar(id, "DTSTART;TZID=Europe/Minsk:20250106T080000", "FREQ=WEEKLY;BYDAY=MO");
        assertReadMatchesCreation(id, body);
    }


    @Test void createRejectsStartDateOutsideSelectedWeekdays() throws Exception {
        assertStartDateError(post(URL), URL, "POST");
        assertThat(api.filtered(URL).size()).isZero();
    }

    @Test void createRejectsPeriodicMassWithoutStartDate() throws Exception {
        assertStartDateError(post(URL), URL, "POST");
        assertThat(masses.count()).isZero();
    }

    @Test void refreshRejectsLegacyPeriodicMassWithoutStartDate() throws Exception {
        String id = savedLegacy("parish-1").getId();
        var before = storedMass(id);
        String path = URL + "/refresh/" + id;

        assertStartDateError(put(path), path, "PUT");

        assertThat(storedMass(id)).isEqualTo(before);
    }

    @Test void createAcceptsStartDateOnAnySelectedWeekday() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isCreated()).andReturn();
        JsonNode created = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String id = created.path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        assertThat(created.path("startDate").asText()).isEqualTo("01/08/2025");
        assertThat(created.path("days")).isEqualTo(json.readTree("[1,3]"));
        assertThat(api.getEntity(URL, id)).isEqualTo(created);
    }

    @Test void replaceRejectsStartDateOutsideSelectedWeekdaysWithoutUpdating() throws Exception {
        String id = saved("parish-1", "01/06/2025");
        JsonNode before = api.getEntity(URL, id);
        assertStartDateError(put(URL + "/" + id), URL + "/" + id, "PUT");
        assertThat(api.filtered(URL)).containsExactly(before);
        assertThat(api.getEntity(URL, id)).isEqualTo(before);
    }

    @Test void replaceAcceptsAlignedStartDate() throws Exception {
        String id = saved("parish-1", "01/06/2025");
        replaceOrRefreshWithAlignedStart(URL + "/" + id, id);
    }

    @Test void refreshAcceptsAlignedStartDateAndNewWeekdaysTogether() throws Exception {
        String id = saved("parish-1", "01/06/2025");
        replaceOrRefreshWithAlignedStart(URL + "/refresh/" + id, id);
    }

    @Test void refreshRejectsExplicitStartDateWithoutUpdating() throws Exception {
        String id = saved("parish-1", "01/06/2025");
        JsonNode before = api.getEntity(URL, id);
        String path = URL + "/refresh/" + id;
        assertStartDateError(put(path), path, "PUT");
        assertThat(api.filtered(URL)).containsExactly(before);
        assertThat(api.getEntity(URL, id)).isEqualTo(before);
    }

    @Test void refreshRejectsWeekdaysThatExcludeExistingStartDateWithoutUpdating() throws Exception {
        String id = api.create(URL, Map.of("cityId", "city-1", "parishId", "parish-1", "langCode", "be",
                "time", "08:00", "days", List.of(1), "startDate", "01/06/2025"));
        JsonNode before = api.getEntity(URL, id);
        String path = URL + "/refresh/" + id;
        assertStartDateError(put(path), path, "PUT");
        assertThat(api.filtered(URL)).containsExactly(before);
        assertThat(api.getEntity(URL, id)).isEqualTo(before);
    }

    private void replaceOrRefreshWithAlignedStart(String path, String id) throws Exception {
        assertJson(mvc.perform(put(path).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn(), id);
        JsonNode updated = api.getEntity(URL, id);
        assertThat(updated.path("startDate").asText()).isEqualTo("01/08/2025");
        assertThat(updated.path("days")).isEqualTo(json.readTree("[1,3]"));
        assertThat(updated.path("time").asText()).isEqualTo("09:00");
    }

    private void assertStartDateError(MockHttpServletRequestBuilder request, String path, String method)
            throws Exception {
        MvcResult result = mvc.perform(request.header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${timestamp}", body.path("timestamp").asText())
                .replace("${path}", path).replace("${method}", method)));
    }

    @Test void createBoundedWeeklyMassKeepsOriginalDatesAndDayOrder() throws Exception {
        String request = new ClassPathResource(
                "feature/mass/schedule/v1/createBoundedWeeklyMassKeepsOriginalDatesAndDayOrder/request.json")
                .getContentAsString(StandardCharsets.UTF_8);
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String id = body.path("id").asText();

        assertThat(body.path("startDate").asText()).isEqualTo("01/03/2025");
        assertThat(body.path("endDate").asText()).isEqualTo("01/10/2025");
        Mass stored = masses.findById(id).orElseThrow();
        assertThat(stored.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 3));
        assertThat(stored.getEndDate()).isEqualTo(LocalDate.of(2025, 1, 10));
        assertThat(stored.getDays()).containsExactly(5, 1);
        assertThat(stored.getDuration()).isEqualTo(2700L);
        assertCalendar(id, "DTSTART;TZID=Europe/Minsk:20250103T080000",
                "FREQ=WEEKLY;BYDAY=MO,FR;UNTIL=20250110T050000Z");
        assertReadMatchesCreation(id, body);
    }

    @Test void storedRuleControlsV1AndWeeklyCalendarWhenLegacyFieldsDisagree() throws Exception {
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setCityId("city-1");
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass mass = savedLegacy(parish.getId());
        mass.setDays(new int[]{1, 5, 3});
        mass.setStartDate(LocalDate.of(2024, 5, 1));
        mass.setEndDate(LocalDate.of(2024, 5, 31));
        mass.setCalendar(new Mass.CalendarDescription("DTSTART;TZID=Europe/Minsk:20240626T113000",
                "FREQ=WEEKLY;BYDAY=WE,FR;UNTIL=20240628T083000Z"));
        masses.save(mass);

        JsonNode detail = json.readTree(mvc.perform(get(URL + "/" + mass.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(detail.path("time").asText()).isEqualTo("11:30");
        assertThat(detail.path("days")).isEqualTo(json.readTree("[5,3]"));
        assertThat(detail.path("startDate").asText()).isEqualTo("05/01/2024");
        assertThat(detail.path("endDate").asText()).isEqualTo("05/31/2024");
        JsonNode filtered = json.readTree(mvc.perform(get(URL).header("X-Api-Key", KEY)
                        .param("filter", "parishId==" + parish.getId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(filtered.size()).isEqualTo(1);
        assertThat(filtered.get(0)).isEqualTo(detail);

        JsonNode week = weekForParish(parish.getId(), "24-06-2024");
        assertThat(week.size()).isEqualTo(2);
        assertThat(week.get(0).path("date").asText()).isEqualTo("06/26/2024");
        assertThat(week.get(1).path("date").asText()).isEqualTo("06/28/2024");
        for (JsonNode day : week) {
            JsonNode hours = day.path("massHours");
            assertThat(hours.size()).isEqualTo(1);
            assertThat(hours.get(0).path("hour").asText()).isEqualTo("11:30");
            assertThat(hours.get(0).path("data").get(0).path("id").asText()).isEqualTo(mass.getId());
        }
        assertThat(weekForParish(parish.getId(), "17-06-2024").isEmpty()).isTrue();
        assertThat(weekForParish(parish.getId(), "01-07-2024").isEmpty()).isTrue();
    }

    @Test void storedSingleRuleUsesItsTimestampAcrossTheLocalDateBoundary() throws Exception {
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setCityId("city-1");
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass mass = savedLegacy(parish.getId());
        mass.setCalendar(new Mass.CalendarDescription("DTSTART:20240624T223000Z",
                "FREQ=DAILY;UNTIL=20240624T223000Z"));
        masses.save(mass);

        JsonNode detail = json.readTree(mvc.perform(get(URL + "/" + mass.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(detail.path("singleStartTimestamp").asLong())
                .isEqualTo(java.time.Instant.parse("2024-06-24T22:30:00Z").getEpochSecond());
        assertThat(detail.path("periodic").asBoolean()).isFalse();
        assertThat(detail.path("time").isNull()).isTrue();
        JsonNode week = weekForParish(parish.getId(), "24-06-2024");
        assertThat(week.size()).isEqualTo(1);
        assertThat(week.get(0).path("date").asText()).isEqualTo("06/25/2024");
        assertThat(week.get(0).path("massHours").get(0).path("hour").asText()).isEqualTo("01:30");
    }

    private JsonNode weekForParish(String parishId, String date) throws Exception {
        MvcResult result = mvc.perform(get(URL + "/week").param("parishId", parishId).param("date", date))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).path("schedule");
    }

    @Test void missingCityIsRejectedWithoutWrite() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void getExistingAndMissingMass() throws Exception {
        String mass = savedLegacy("parish-1").getId();
        assertThat(storedMass(mass)).doesNotContainKey("calendar");
        JsonNode body = json.readTree(mvc.perform(get(URL + "/" + mass).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(body.path("id").asText()).isEqualTo(mass);
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${id}", mass)
                .replace("${lastModifiedDate}", checkedModified(body))));
        mvc.perform(get(URL + "/000000000000000000000000").header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
    }

    @Test void replaceMassUpdatesStoredTime() throws Exception {
        String mass = saved("parish-1", "01/06/2025");
        assertJson(mvc.perform(put(URL + "/" + mass).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn(), mass);
        assertThat(api.getEntity(V1TestApi.MASSES, mass).path("time").asText()).isEqualTo("09:00");
        assertCalendar(mass, "DTSTART;TZID=Europe/Minsk:20250106T090000", "FREQ=WEEKLY;BYDAY=MO");
    }

    @Test void refreshMassChangesOnlyProvidedField() throws Exception {
        String mass = saved("parish-1", "01/06/2025");
        assertJson(mvc.perform(put(URL + "/refresh/" + mass).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn(), mass);
        JsonNode updated = api.getEntity(V1TestApi.MASSES, mass);
        assertThat(updated.path("time").asText()).isEqualTo("09:00");
        assertThat(updated.path("cityId").asText()).isEqualTo("city-1");
        assertThat(updated.path("parishId").asText()).isEqualTo("parish-1");
        assertCalendar(mass, "DTSTART;TZID=Europe/Minsk:20250106T090000", "FREQ=WEEKLY;BYDAY=MO");
    }

    @Test void deleteMassRemovesIt() throws Exception {
        String mass = saved("parish-1", "01/06/2025");
        assertJson(mvc.perform(delete(URL + "/" + mass).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn(), mass);
        api.expectMissing(V1TestApi.MASSES, mass);
    }

    @Test void missingMassCannotBeUpdatedOrDeleted() throws Exception {
        String id = "000000000000000000000000";
        mvc.perform(put(URL + "/" + id).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isNotFound());
        mvc.perform(delete(URL + "/" + id).header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void invalidRemovalIntervalPreservesMass() throws Exception {
        String mass = saved("parish-1", "01/06/2025");
        mvc.perform(delete(URL + "/" + mass).param("from", "02-01-2025")
                        .param("to", "01-01-2025").header("X-Api-Key", KEY))
                .andExpect(status().isBadRequest());
        api.getEntity(V1TestApi.MASSES, mass);
    }

    @Test void removalFromDateWithoutToTruncatesPeriodicMass() throws Exception {
        String mass = api.create(URL, Map.of(
                "startDate", "01/06/2025",
                "endDate", "01/31/2025",
                "cityId", "city-1",
                "parishId", "parish-1",
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1}));
        MvcResult result = mvc.perform(delete(URL + "/" + mass).param("from", "08-01-2025")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", mass)));
        JsonNode remaining = api.getEntity(V1TestApi.MASSES, mass);
        assertThat(remaining.path("startDate").asText()).isEqualTo("01/06/2025");
        assertThat(remaining.path("endDate").asText()).isEqualTo("01/07/2025");
        assertThat(remaining.path("days")).isEqualTo(json.readTree("[1]"));
        assertThat(api.filtered(V1TestApi.MASSES).size()).isEqualTo(1);
    }

    @Test void removeAllMassesByParish() throws Exception {
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "cityId", "city-1",
                "key", "fixture-parish"));
        String first = saved(parish, "06/24/2024");
        MvcResult result = mvc.perform(delete(URL).param("parishId", parish)
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("status").asText()).isEqualTo("DELETED");
        assertThat(body.path("entities").get(0).asText()).isEqualTo(first);
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void cachedMassReadAfterBulkDeleteReturnsNotFound() throws Exception {
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "cityId", "city-1",
                "key", "fixture-parish"));
        String mass = saved(parish, "06/24/2024");
        JsonNode first = json.readTree(mvc.perform(get(URL + "/" + mass).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(first.path("id").asText()).isEqualTo(mass);
        JsonNode expected = json.readTree(fixture("response.json")
                .replace("${id}", mass)
                .replace("${parishId}", parish)
                .replace("${lastModifiedDate}", checkedModified(first)));
        assertThat(first).isEqualTo(expected);
        MvcResult deletion = mvc.perform(delete(URL).param("parishId", parish)
                .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(deletion.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("entities").get(0).asText()).isEqualTo(mass);
        api.expectMissing(V1TestApi.MASSES, mass);
    }

    @Test void anonymousCannotCreateMass() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isUnauthorized());
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void anonymousCannotDeleteMass() throws Exception {
        String mass = saved("parish-1", "01/06/2025");
        mvc.perform(delete(URL + "/" + mass)).andExpect(status().isUnauthorized());
        api.getEntity(V1TestApi.MASSES, mass);
    }

    @Test void createSingleMassUsesTimestamp() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String id = body.path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${id}", id).replace("${lastModifiedDate}", checkedModified(body))));
        JsonNode stored = api.getEntity(V1TestApi.MASSES, id);
        assertThat(stored.path("singleStartTimestamp").asLong()).isEqualTo(1_700_000_000L);
        assertThat(stored.path("time")).isEqualTo(json.nullNode());
        assertCalendar(id, "DTSTART:20231114T221320Z", "FREQ=DAILY;UNTIL=20231114T221320Z");
        assertReadMatchesCreation(id, body);
    }

    @Test void singleMassAcceptsStartDateWithoutWeekdays() throws Exception {
        String id = api.create(URL, Map.of("cityId", "city-1", "parishId", "parish-1", "langCode", "be",
                "singleStartTimestamp", 1_700_000_000L, "startDate", "01/08/2025"));
        JsonNode single = api.getEntity(URL, id);
        assertThat(single.path("periodic").asBoolean()).isFalse();
        assertThat(single.path("singleStartTimestamp").asLong()).isEqualTo(1_700_000_000L);
        assertThat(single.path("startDate").asText()).isEqualTo("01/08/2025");
        assertThat(single.path("days").isNull()).isTrue();
    }

    @Test void singleMassRejectsTime() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest()).andReturn();
        assertValidationError(result, "MASS.007", "time");
    }

    @Test void singleMassRejectsNonEmptyDays() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest()).andReturn();
        assertValidationError(result, "MASS.009", "days");
    }

    @Test void periodicMassRejectsEmptyDays() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest()).andReturn();
        assertValidationError(result, "MASS.008", "days");
    }

    @Test void periodicMassRejectsDuplicateDays() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest()).andReturn();
        assertValidationError(result, "MASS.002", "days");
    }

    @Test void periodicMassRejectsDayOutsideShortDateRange() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest()).andReturn();
        assertValidationError(result, "MASS.002", "days");
    }

    @Test void missingParishIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void invalidPeriodicTimeIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void missingPeriodicDaysAreRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void refreshMissingMassReturns404() throws Exception {
        mvc.perform(put(URL + "/refresh/000000000000000000000000")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isNotFound());
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void refreshWithoutBodyKeepsMass() throws Exception {
        String mass = saved("parish-1", "01/06/2025");
        mvc.perform(put(URL + "/refresh/" + mass).header("X-Api-Key", KEY))
                .andExpect(status().isOk());
        JsonNode updated = api.getEntity(V1TestApi.MASSES, mass);
        assertThat(updated.path("time").asText()).isEqualTo("08:00");
        assertThat(updated.path("days")).isEqualTo(json.readTree("[1]"));
    }

    @Test void bulkRefreshUpdatesParishTimestamp() throws Exception {
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "cityId", "city-1",
                "key", "fixture-parish"));
        when(dateTimeProvider.nowSystemDefaultZone()).thenReturn(LocalDateTime.of(2023, 7, 17, 10, 0));
        MvcResult result = mvc.perform(put(URL).param("parishId", parish)
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("entities").get(0).asText()).isEqualTo(parish);
        assertThat(body.path("status").asText()).isEqualTo("UPDATED");
        assertThat(api.getEntity(V1TestApi.PARISHES, parish).path("lastConfirmRelevance").asText()).isEqualTo("17-07-2023 10:00:00");
    }

    @Test void bulkDeleteRejectsUnknownParish() throws Exception {
        mvc.perform(delete(URL).param("parishId", "000000000000000000000000")
                        .header("X-Api-Key", KEY)).andExpect(status().isNotFound());
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void deleteWholePeriodicMassThroughInterval() throws Exception {
        String mass = api.create(URL, Map.of(
                "startDate", "01/06/2025",
                "endDate", "01/31/2025",
                "cityId", "city-1",
                "parishId", "parish-1",
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1}));
        MvcResult result = mvc.perform(delete(URL + "/" + mass)
                        .param("from", "31-12-2024").param("to", "01-02-2025")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).path("status").asText()).isEqualTo("DELETED");
        assertThat(body.get(0).path("id").asText()).isEqualTo(mass);
        api.expectMissing(V1TestApi.MASSES, mass);
    }

    @Test void removeMiddleOfPeriodicMassSplitsStoredRange() throws Exception {
        String original = api.create(URL, Map.of(
                "startDate", "01/06/2025",
                "endDate", "01/31/2025",
                "cityId", "city-1",
                "parishId", "parish-1",
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1}));
        assertThat(api.getEntity(URL, original).path("endDate").asText()).isEqualTo("01/31/2025");
        var stored = masses.findById(original).orElseThrow();
        stored.setEndDate(LocalDate.of(2025, 2, 3));
        masses.save(stored);
        MvcResult result = mvc.perform(delete(URL + "/" + original)
                        .param("from", "08-01-2025").param("to", "14-01-2025")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.size()).isEqualTo(2);
        assertThat(body.get(0).path("id").asText()).isEqualTo(original);
        assertThat(body.get(0).path("status").asText()).isEqualTo("UPDATED");
        String createdId = body.get(1).path("id").asText();
        assertThat(createdId).matches("[0-9a-f]{24}").isNotEqualTo(original);
        assertThat(body.get(1).path("status").asText()).isEqualTo("CREATED");
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")
                        .replace("${originalId}", original).replace("${createdId}", createdId)));
        assertThat(api.filtered(V1TestApi.MASSES).size()).isEqualTo(2);
        JsonNode before = api.getEntity(V1TestApi.MASSES, original);
        JsonNode after = api.getEntity(V1TestApi.MASSES, createdId);
        assertThat(before.path("startDate").asText()).isEqualTo("01/06/2025");
        assertThat(before.path("endDate").asText()).isEqualTo("01/07/2025");
        assertThat(after.path("startDate").asText()).isEqualTo("01/15/2025");
        assertThat(after.path("endDate").asText()).isEqualTo("02/03/2025");
        assertThat(before.path("days")).isEqualTo(json.readTree("[1]"));
        assertThat(after.path("days")).isEqualTo(json.readTree("[1]"));
        assertCalendar(original, "DTSTART;TZID=Europe/Minsk:20250106T080000",
                "FREQ=WEEKLY;BYDAY=MO;UNTIL=20250107T050000Z");
        assertCalendar(createdId, "DTSTART;TZID=Europe/Minsk:20250120T080000",
                "FREQ=WEEKLY;BYDAY=MO;UNTIL=20250203T050000Z");
        mvc.perform(put(URL + "/refresh/" + createdId).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("refresh-request.json")))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.errors[0].code").value("MASS.013"));
        assertThat(api.getEntity(URL, createdId)).isEqualTo(after);
        mvc.perform(put(URL + "/refresh/" + createdId).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("startDate", "2025-01-20", "notes", "Updated after split"))))
                .andExpect(status().isOk());
        JsonNode refreshed = api.getEntity(URL, createdId);
        assertThat(refreshed.path("startDate").asText()).isEqualTo("01/20/2025");
        assertThat(refreshed.path("days")).isEqualTo(after.path("days"));
        assertThat(refreshed.path("notes").asText()).isEqualTo("Updated after split");
    }

    @Test void refreshPreservesRequestedWeekdayOrderAfterIntervalRemoval() throws Exception {
        String id = api.create(URL, Map.of("cityId", "city-1", "parishId", "parish-1", "langCode", "be",
                "time", "08:00", "days", List.of(1, 5), "startDate", "01/06/2025", "endDate", "01/31/2025"));
        mvc.perform(delete(URL + "/" + id).header("X-Api-Key", KEY)
                        .param("from", "06-01-2025").param("to", "16-01-2025"))
                .andExpect(status().isOk());
        JsonNode before = api.getEntity(URL, id);
        assertThat(before.path("startDate").asText()).isEqualTo("01/17/2025");
        assertThat(before.path("days")).isEqualTo(json.readTree("[1,5]"));

        assertJson(mvc.perform(put(URL + "/refresh/" + id).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn(), id);
        JsonNode after = api.getEntity(URL, id);
        assertThat(after.path("startDate")).isEqualTo(before.path("startDate"));
        assertThat(after.path("endDate")).isEqualTo(before.path("endDate"));
        assertThat(after.path("days")).isEqualTo(json.readTree("[5,1]"));
    }

    @Test void removeMiddlePreservesStartAndActualOccurrences() throws Exception {
        String city = api.city("minsk", "Minsk");
        String parish = api.parish("APPROVED", Map.of("cityId", city, "name", "St. Mary", "key", "st-mary"));
        String id = api.create(URL, Map.of("cityId", city, "parishId", parish, "langCode", "be",
                "time", "08:00", "days", List.of(5, 1), "startDate", "01/03/2053"));
        MvcResult deletion = mvc.perform(delete(URL + "/" + id).header("X-Api-Key", KEY)
                        .param("from", "08-01-2053").param("to", "14-01-2053"))
                .andExpect(status().isOk()).andReturn();
        JsonNode changes = json.readTree(deletion.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(changes.size()).isEqualTo(2);
        String rightId = changes.get(1).path("id").asText();
        assertThat(rightId).matches("[0-9a-f]{24}").isNotEqualTo(id);
        assertThat(changes).isEqualTo(json.readTree(fixture("response.json")
                .replace("${originalId}", id).replace("${createdId}", rightId)));

        JsonNode left = api.getEntity(URL, id);
        JsonNode right = api.getEntity(URL, rightId);
        assertThat(left.path("startDate").asText()).isEqualTo("01/03/2053");
        assertThat(left.path("endDate").asText()).isEqualTo("01/07/2053");
        assertThat(right.path("startDate").asText()).isEqualTo("01/15/2053");
        assertThat(right.path("endDate").isNull()).isTrue();
        assertThat(left.path("days")).isEqualTo(json.readTree("[5,1]"));
        assertThat(right.path("days")).isEqualTo(json.readTree("[5,1]"));
        assertWeekDates(parish, "06-01-2053", "01/06/2053");
        assertWeekDates(parish, "13-01-2053", "01/17/2053");
        assertWeekDates(parish, "20-01-2053", "01/20/2053", "01/24/2053");
    }

    private void assertWeekDates(String parish, String date, String... expected) throws Exception {
        MvcResult result = mvc.perform(get(URL + "/week").param("parishId", parish).param("date", date))
                .andExpect(status().isOk()).andReturn();
        JsonNode week = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(week.path("schedule").values()).extracting(day -> day.path("date").asText())
                .containsExactly(expected);
    }

    @Test void removeBeginningDeletesSeriesWhenNoOccurrenceRemains() throws Exception {
        String id = api.create(URL, Map.of("cityId", "city-1", "parishId", "parish-1", "langCode", "be",
                "time", "08:00", "days", List.of(1, 5), "startDate", "01/06/2025", "endDate", "01/12/2025"));
        assertJson(mvc.perform(delete(URL + "/" + id).header("X-Api-Key", KEY)
                        .param("from", "06-01-2025").param("to", "10-01-2025"))
                .andExpect(status().isOk()).andReturn(), id);
        api.expectMissing(URL, id);
        assertThat(api.filtered(URL).size()).isZero();
    }

    @Test void removeStartOfPeriodicMassKeepsOriginalId() throws Exception {
        String mass = api.create(URL, Map.of(
                "startDate", "01/06/2025",
                "endDate", "01/31/2025",
                "cityId", "city-1",
                "parishId", "parish-1",
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1}));
        MvcResult result = mvc.perform(delete(URL + "/" + mass).param("from", "06-01-2025")
                        .param("to", "07-01-2025").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).path("id").asText()).isEqualTo(mass);
        assertThat(body.get(0).path("status").asText()).isEqualTo("UPDATED");
        assertThat(api.filtered(V1TestApi.MASSES).size()).isEqualTo(1);
        JsonNode remaining = api.getEntity(V1TestApi.MASSES, mass);
        assertThat(remaining.path("startDate").asText()).isEqualTo("01/08/2025");
        assertThat(remaining.path("endDate").asText()).isEqualTo("01/31/2025");
        assertThat(remaining.path("days")).isEqualTo(json.readTree("[1]"));
    }

    @Test void removeOnlyWeekdayDeletesShortPeriodicRemainder() throws Exception {
        String mass = api.create(URL, Map.of(
                "startDate", "01/06/2025",
                "endDate", "01/08/2025",
                "cityId", "city-1",
                "parishId", "parish-1",
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1}));

        MvcResult result = mvc.perform(delete(URL + "/" + mass)
                        .param("from", "06-01-2025").param("to", "06-01-2025")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();

        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", mass)));
        api.expectMissing(V1TestApi.MASSES, mass);
    }

    @Test void removeEndOfPeriodicMassKeepsOriginalId() throws Exception {
        String mass = api.create(URL, Map.of(
                "startDate", "01/06/2025",
                "endDate", "01/31/2025",
                "cityId", "city-1",
                "parishId", "parish-1",
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1}));
        MvcResult result = mvc.perform(delete(URL + "/" + mass).param("from", "25-01-2025")
                        .param("to", "31-01-2025").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).path("id").asText()).isEqualTo(mass);
        assertThat(body.get(0).path("status").asText()).isEqualTo("UPDATED");
        assertThat(api.filtered(V1TestApi.MASSES).size()).isEqualTo(1);
        JsonNode remaining = api.getEntity(V1TestApi.MASSES, mass);
        assertThat(remaining.path("startDate").asText()).isEqualTo("01/06/2025");
        assertThat(remaining.path("endDate").asText()).isEqualTo("01/24/2025");
        assertThat(remaining.path("days")).isEqualTo(json.readTree("[1]"));
    }

    @Test void filterRequiresExpression() throws Exception {
        MvcResult result = mvc.perform(get(URL).header("X-Api-Key", KEY)).andExpect(status().isBadRequest()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString())).isEqualTo(json.readTree(fixture("response.json")));
    }

    @Test void malformedFilterIsRejected() throws Exception {
        MvcResult result = mvc.perform(get(URL).param("filter", "parishId==").header("X-Api-Key", KEY))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString()).path("errors").isEmpty()).isTrue();
    }

    @Test void unknownParishWeekKeysReturn404() throws Exception {
        mvc.perform(get(URL + "/parish-week").param("apiKey", "unknown"))
                .andExpect(status().isNotFound());
        mvc.perform(get(URL + "/parish-week").header("parish-week-api-key", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test void duplicatePeriodicMassIsRejected() throws Exception {
        String existing = saved("parish-1", "01/06/2025");
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.MASSES).size()).isEqualTo(1);
        api.getEntity(V1TestApi.MASSES, existing);
    }

    @Test void invalidWeekdayIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.MASSES).size()).isZero();
    }

    @Test void filterReturnsMatchingMass() throws Exception {
        String expected = saved("parish-1", "01/06/2025");
        api.create(URL, Map.of(
                "cityId", "city-1",
                "parishId", "parish-2",
                "langCode", "be",
                "time", "09:00",
                "days", new int[]{1},
                "startDate", "01/06/2025"));
        MvcResult result = mvc.perform(get(URL).param("filter", "parishId==parish-1")
                        .param("offset", "0").param("limit", "1")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).path("id").asText()).isEqualTo(expected);
    }

    @Test void mapReturnsIndexedCoordinates() throws Exception {
        when(meilisearchReader.searchNearestMasses(argThat(filter -> filter != null
                        && OffsetDateTime.parse("2024-06-24T00:00:00+03:00").equals(filter.dateTimeFrom())
                        && LocalDate.of(2024, 6, 24).equals(filter.dateTimeTo().toLocalDate()))))
                .thenReturn(SearchResult.builder().hits(List.of(indexedMass("city-1", "parish-1")))
                        .facetDistribution(Map.of()).build());
        MvcResult result = mvc.perform(get(URL + "/map")
                        .param("dateTimeFrom", "2024-06-24T00:00:00+03:00"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
    }

    @Test void mapReturnsUnapprovedParishes() throws Exception {
        String city = api.city("minsk", "Minsk");
        String pending = api.parish("PENDING", Map.of(
                "name", "Pending parish",
                "address", "Church street 1",
                "imgPath", "/pending.jpg",
                "gps", Map.of("latitude", 53.5, "longitude", 27.25),
                "cityId", city,
                "key", "pending-parish"));
        for (String state : List.of("APPROVED", "DISABLED")) {
            api.parish(state, Map.of(
                    "name", state,
                    "cityId", city,
                    "key", state.toLowerCase()));
        }
        when(meilisearchReader.searchNearestMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());
        MvcResult result = mvc.perform(get(URL + "/map")
                        .param("dateTimeFrom", "2024-06-24T00:00:00+03:00"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", pending)));
    }

    @Test void mapRequiresDateTimeFrom() throws Exception {
        MvcResult result = mvc.perform(get(URL + "/map")).andExpect(status().isBadRequest()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
    }

    @Test void mapRejectsMalformedDateTimeFrom() throws Exception {
        MvcResult result = mvc.perform(get(URL + "/map").param("dateTimeFrom", "not-a-date"))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
    }

    @Test void mapPassesExplicitEndAndReturnsUnapprovedParish() throws Exception {
        String pending = api.parish("PENDING", Map.of(
                "name", "Pending",
                "cityId", "city-1",
                "key", "fixture-parish"));
        when(meilisearchReader.searchNearestMasses(argThat(filter -> filter != null
                        && OffsetDateTime.parse("2024-06-25T12:00:00+03:00").equals(filter.dateTimeTo()))))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());
        MvcResult result = mvc.perform(get(URL + "/map").param("dateTimeFrom", "2024-06-24T00:00:00+03:00")
                        .param("dateTimeTo", "2024-06-25T12:00:00+03:00"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", pending)));
    }

    @Test void mapUnapprovedParishesCacheRefreshesAfterStateChange() throws Exception {
        String pending = api.parish("PENDING", Map.of(
                "name", "Pending",
                "cityId", "city-1",
                "key", "fixture-parish"));
        when(meilisearchReader.searchNearestMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());
        String path = URL + "/map";
        String date = "2024-06-24T00:00:00+03:00";
        JsonNode initial = json.readTree(mvc.perform(get(path).param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(initial).isEqualTo(json.readTree(fixture("initial-response.json").replace("${id}", pending)));
        JsonNode repeated = json.readTree(mvc.perform(get(path).param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(repeated).isEqualTo(initial);
        mvc.perform(put("/api/parish/{id}/state", pending).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("state-request.json")))
                .andExpect(status().isOk());
        assertThat(api.getEntity(V1TestApi.PARISHES, pending).path("state").asText())
                .isEqualTo("APPROVED");
        JsonNode after = json.readTree(mvc.perform(get(path).param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(after).isEqualTo(json.readTree(fixture("after-response.json")));
    }

    @Test void mapUnapprovedParishesCacheRefreshesAfterSaveAndDelete() throws Exception {
        when(meilisearchReader.searchNearestMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());
        String date = "2024-06-24T00:00:00+03:00";
        MvcResult empty = mvc.perform(get(URL + "/map").param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(empty.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("empty-response.json")));

        String pending = api.parish("PENDING", Map.of(
                "name", "Saved pending",
                "cityId", "city-1",
                "key", "fixture-parish"));
        MvcResult added = mvc.perform(get(URL + "/map").param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(added.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("added-response.json").replace("${id}", pending)));

        mvc.perform(delete(V1TestApi.PARISHES + "/" + pending).header("X-Api-Key", KEY))
                .andExpect(status().isOk());
        MvcResult deleted = mvc.perform(get(URL + "/map").param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(deleted.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("empty-response.json")));
    }

    @Test void weekReturnsEmptyScheduleForUnknownParish() throws Exception {
        MvcResult result = mvc.perform(get(URL + "/week")
                        .param("parishId", "missing_parish_id")
                        .param("date", "24-06-2024"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
    }

    @Test void weekKeepsNavigationForParishWithoutMasses() throws Exception {
        String city = api.city("minsk", "Minsk");
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "cityId", city,
                "key", "st-mary"));

        MvcResult result = mvc.perform(get(URL + "/week")
                        .param("parishId", parish).param("date", "24-06-2024")
                        .param("online", "true").param("rorate", "true").param("massLang", "pl"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${cityId}", city).replace("${parishId}", parish)));
    }

    @Test void indexedWeekKeepsNavigationForParishWithoutMasses() throws Exception {
        String city = api.city("minsk", "Minsk");
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "cityId", city,
                "key", "st-mary"));
        when(meilisearchReader.searchAllMasses(argThat(filter -> filter != null
                        && city.equals(filter.cityId())
                        && parish.equals(filter.parishId()))))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());

        MvcResult result = mvc.perform(get(URL + "/week-indexed")
                        .cookie(new Cookie("cityId", city))
                        .param("parishId", parish).param("date", "24-06-2024")
                        .param("online", "true").param("rorate", "true").param("massLang", "pl"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${cityId}", city).replace("${parishId}", parish)));
    }

    @Test void weekDefaultsToTodayAndDefaultCityWithoutCookie() throws Exception {
        String city = api.city("minsk", "Minsk");
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "key", "st-mary",
                "cityId", city));
        String mass = api.create(URL, Map.of(
                "days", new int[]{7},
                "startDate", "07/09/2023",
                "cityId", city,
                "parishId", parish,
                "langCode", "be",
                "time", "08:00"));
        MvcResult result = mvc.perform(get(URL + "/week"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body).isEqualTo(json.readTree(fixture("response.json").replace("${cityId}", city)
                .replace("${lastConfirmRelevance}", parishConfirmation(parish))
                .replace("${parishId}", parish)
                .replace("${massId}", mass)
                .replace("${lastModifiedDate}", checkedModified(body.path("schedule").get(0)
                        .path("massHours").get(0).path("data").get(0)))));
    }

    @Test void cachedCityWeekAfterBulkDeleteHasNoMasses() throws Exception {
        String city = api.city("minsk", "Minsk");
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "key", "st-mary",
                "cityId", city));
        String mass = api.create(URL, Map.of(
                "days", new int[]{7},
                "startDate", "07/09/2023",
                "cityId", city,
                "parishId", parish,
                "langCode", "be",
                "time", "08:00"));
        MvcResult first = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        JsonNode expected = json.readTree(fixture("response.json").replace("${cityId}", city)
                .replace("${lastConfirmRelevance}", parishConfirmation(parish))
                .replace("${parishId}", parish)
                .replace("${massId}", mass)
                .replace("${lastModifiedDate}", checkedModified(json.readTree(first.getResponse()
                        .getContentAsString(StandardCharsets.UTF_8)).path("schedule").get(0)
                        .path("massHours").get(0).path("data").get(0))));
        assertThat(json.readTree(first.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(expected);
        MvcResult deletion = mvc.perform(delete(URL).param("parishId", parish)
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(deletion.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("delete-response.json").replace("${massId}", mass)));
        api.expectMissing(V1TestApi.MASSES, mass);
        MvcResult after = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(after.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("schedule").isEmpty()).isTrue();
    }

    @Test void cachedCityWeekExcludesParishAfterItBecomesPending() throws Exception {
        String city = api.city("minsk", "Minsk");
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "key", "st-mary",
                "cityId", city));
        String mass = api.create(URL, Map.of(
                "days", new int[]{7},
                "startDate", "07/09/2023",
                "cityId", city,
                "parishId", parish,
                "langCode", "be",
                "time", "08:00"));
        MvcResult first = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        JsonNode expected = json.readTree(fixture("response.json").replace("${cityId}", city)
                .replace("${lastConfirmRelevance}", parishConfirmation(parish))
                .replace("${parishId}", parish)
                .replace("${massId}", mass)
                .replace("${lastModifiedDate}", checkedModified(json.readTree(first.getResponse()
                        .getContentAsString(StandardCharsets.UTF_8)).path("schedule").get(0)
                        .path("massHours").get(0).path("data").get(0))));
        assertThat(json.readTree(first.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(expected);
        MvcResult state = mvc.perform(put("/api/parish/{id}/state", parish)
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("state-request.json")))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(state.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("state-response.json").replace("${parishId}", parish)));
        assertThat(api.getEntity(V1TestApi.PARISHES, parish).path("state").asText())
                .isEqualTo("PENDING");
        api.getEntity(V1TestApi.MASSES, mass);
        MvcResult after = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(after.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("after-response.json").replace("${cityId}", city)));
    }

    @Test void cachedCityWeekIncludesParishAfterItBecomesApproved() throws Exception {
        String city = api.city("minsk", "Minsk");
        String parish = api.parish("PENDING", Map.of(
                "name", "St. Mary",
                "key", "st-mary",
                "cityId", city));
        String mass = api.create(URL, Map.of(
                "days", new int[]{7},
                "startDate", "07/09/2023",
                "cityId", city,
                "parishId", parish,
                "langCode", "be",
                "time", "08:00"));
        MvcResult first = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(first.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("before-response.json").replace("${cityId}", city)));
        MvcResult state = mvc.perform(put("/api/parish/{id}/state", parish)
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("state-request.json")))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(state.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("state-response.json").replace("${parishId}", parish)));
        assertThat(api.getEntity(V1TestApi.PARISHES, parish).path("state").asText())
                .isEqualTo("APPROVED");
        api.getEntity(V1TestApi.MASSES, mass);
        MvcResult after = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(after.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body).isEqualTo(json.readTree(fixture("response.json").replace("${cityId}", city)
                .replace("${lastConfirmRelevance}", parishConfirmation(parish))
                .replace("${parishId}", parish)
                .replace("${massId}", mass)
                .replace("${lastModifiedDate}", checkedModified(body.path("schedule").get(0)
                        .path("massHours").get(0).path("data").get(0)))));
    }

    @Test void indexedWeekDefaultsToTodayAndDefaultCityWithoutCookie() throws Exception {
        String city = api.city("minsk", "Minsk");
        when(meilisearchReader.searchAllMasses(argThat(filter -> filter != null
                        && city.equals(filter.cityId())
                        && OffsetDateTime.parse("2023-07-16T00:00:00+03:00").equals(filter.dateTimeFrom())
                        && LocalDate.of(2023, 7, 22).equals(filter.dateTimeTo().toLocalDate()))))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());
        JsonNode body = json.readTree(mvc.perform(get(URL + "/week-indexed"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.path("startWeekDate").asText()).isEqualTo("07/16/2023");
        assertThat(body.path("schedule").isEmpty()).isTrue();
    }

    @Test void weekWithoutParishUsesCityAndExcludesPendingParishes() throws Exception {
        String selected = api.city("selected", "Selected");
        String otherCity = api.city("other", "Other");
        String approved = api.parish("APPROVED", Map.of(
                "name", "Approved",
                "key", "approved",
                "cityId", selected));
        String pending = api.parish("PENDING", Map.of(
                "name", "Pending",
                "key", "pending",
                "cityId", selected));
        String elsewhere = api.parish("APPROVED", Map.of(
                "name", "Elsewhere",
                "key", "elsewhere",
                "cityId", otherCity));
        String visible = api.create(URL, Map.of(
                "cityId", selected,
                "parishId", approved,
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1},
                "startDate", "06/24/2024"));
        api.create(URL, Map.of(
                "cityId", selected,
                "parishId", pending,
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1},
                "startDate", "06/24/2024"));
        api.create(URL, Map.of(
                "cityId", otherCity,
                "parishId", elsewhere,
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1},
                "startDate", "06/24/2024"));
        JsonNode result = json.readTree(mvc.perform(get(URL + "/week")
                        .cookie(new Cookie("cityId", selected)).param("date", "24-06-2024"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(result.path("schedule").size()).isEqualTo(1);
        assertThat(result.path("schedule").get(0).path("massHours").get(0).path("data").get(0).path("id").asText())
                .isEqualTo(visible);
    }

    @Test void weekReturnsApprovedParishMass() throws Exception {
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "cityId", "city-1",
                "key", "fixture-parish"));
        String mass = saved(parish, "06/24/2024");
        MvcResult result = mvc.perform(get(URL + "/week")
                        .param("parishId", parish).param("date", "24-06-2024"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${lastConfirmRelevance}", parishConfirmation(parish))
                .replace("${parishId}", parish)
                .replace("${massId}", mass)
                .replace("${lastModifiedDate}", checkedModified(body.path("schedule").get(0).path("massHours").get(0).path("data").get(0)))));
    }

    @Test void weekFiltersOnlineRorateAndLanguage() throws Exception {
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "cityId", "city-1",
                "key", "fixture-parish"));
        String matching = api.create(URL, Map.of(
                "online", true,
                "rorate", true,
                "cityId", "city-1",
                "parishId", parish,
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1},
                "startDate", "06/24/2024"));
        createFilteredMass(parish, "09:00", "be", false, true);
        createFilteredMass(parish, "10:00", "pl", true, true);
        createFilteredMass(parish, "11:00", "be", true, false);
        JsonNode body = json.readTree(mvc.perform(get(URL + "/week")
                        .param("parishId", parish).param("date", "24-06-2024")
                        .param("online", "true").param("rorate", "true").param("massLang", "be"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.path("schedule").size()).isEqualTo(1);
        JsonNode data = body.path("schedule").get(0).path("massHours").get(0).path("data");
        assertThat(data.size()).isEqualTo(1);
        assertThat(data.get(0).path("id").asText()).isEqualTo(matching);
        assertThat(body.path("nav").path("selected").path("online").path("value").asText()).isEqualTo("true");
        assertThat(body.path("nav").path("selected").path("rorate").path("value").asText()).isEqualTo("true");
        assertThat(body.path("nav").path("selected").path("lang").path("value").asText()).isEqualTo("be");
    }

    @Test void weekIncludesPendingParishOnlyWhenRequested() throws Exception {
        String parish = api.parish("PENDING", Map.of(
                "name", "Pending parish",
                "cityId", "city-1",
                "key", "fixture-parish"));
        String mass = saved(parish, "06/24/2024");
        JsonNode hidden = json.readTree(mvc.perform(get(URL + "/week")
                        .param("parishId", parish).param("date", "24-06-2024"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(hidden.path("schedule").isEmpty()).isTrue();
        JsonNode shown = json.readTree(mvc.perform(get(URL + "/week")
                        .param("parishId", parish).param("date", "24-06-2024")
                        .header("x-show-pending", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(shown.path("schedule").size()).isEqualTo(1);
        assertThat(shown.path("schedule").get(0).path("massHours").get(0).path("data").get(0).path("id").asText())
                .isEqualTo(mass);
    }

    @Test void indexedWeekReturnsIndexedMass() throws Exception {
        String city = api.city("minsk", "Minsk");
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "key", "st-mary",
                "cityId", city));
        when(meilisearchReader.searchAllMasses(argThat(filter -> filter != null
                        && city.equals(filter.cityId())
                        && parish.equals(filter.parishId())
                        && LocalDate.of(2024, 6, 24).equals(filter.dateTimeFrom().toLocalDate())
                        && LocalDate.of(2024, 6, 30).equals(filter.dateTimeTo().toLocalDate())
                        && filter.online() == null
                        && filter.rorate() == null
                        && filter.lang() == null)))
                .thenReturn(SearchResult.builder().hits(List.of(indexedMass(city, parish)))
                        .facetDistribution(Map.of()).build());
        MvcResult result = mvc.perform(get(URL + "/week-indexed")
                        .param("parishId", parish)
                        .cookie(new Cookie("cityId", city))
                        .param("date", "24-06-2024"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${cityId}", city).replace("${parishId}", parish)
                .replace("${lastConfirmRelevance}", parishConfirmation(parish))));
    }

    @Test void indexedWeekPassesFilters() throws Exception {
        String city = api.city("minsk", "Minsk");
        String parish = api.parish("APPROVED", Map.of(
                "name", "Approved",
                "cityId", city,
                "key", "fixture-parish"));
        SearchResultItem hit = new SearchResultItem("record-1", "mass-1", 60,
                LocalDateTime.of(2024, 6, 24, 8, 0),
                new by.imsha.meilisearch.model.Parish(parish, true, "APPROVED"),
                "Morning mass", "be", true, true,
                new by.imsha.meilisearch.model.City(city), null,
                new Geo(53.9, 27.57), 120L);
        when(meilisearchReader.searchAllMasses(argThat(filter -> filter != null
                        && Boolean.TRUE.equals(filter.online())
                        && Boolean.TRUE.equals(filter.rorate())
                        && "be".equals(filter.lang()))))
                .thenReturn(SearchResult.builder().hits(List.of(hit)).facetDistribution(Map.of()).build());
        JsonNode result = json.readTree(mvc.perform(get(URL + "/week-indexed")
                        .cookie(new Cookie("cityId", city)).param("date", "24-06-2024")
                        .param("online", "true").param("rorate", "true").param("massLang", "be")
                        .header("x-show-pending", "false"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(result.path("schedule").size()).isEqualTo(1);
    }

    @Test void parishWeekQueryKeyReturnsSchedule() throws Exception {
        String parish = api.parish("APPROVED", Map.of(
                "name", "Mapped parish",
                "key", "123",
                "cityId", "city-1"));
        String mass = saved(parish, "07/10/2023");
        MvcResult result = mvc.perform(get(URL + "/parish-week").param("apiKey", "123"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode data = body.get(0).path("massHours").get(0).path("data").get(0);
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${lastConfirmRelevance}", parishConfirmation(parish))
                .replace("${parishId}", parish)
                .replace("${massId}", mass)
                .replace("${lastModifiedDate}", checkedModified(data))));
    }

    @Test void parishWeekHeaderKeyReturnsSchedule() throws Exception {
        String parish = api.parish("APPROVED", Map.of(
                "name", "Mapped parish",
                "key", "123",
                "cityId", "city-1"));
        String mass = saved(parish, "07/10/2023");
        MvcResult result = mvc.perform(get(URL + "/parish-week").header("parish-week-api-key", "123"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode data = body.get(0).path("massHours").get(0).path("data").get(0);
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${lastConfirmRelevance}", parishConfirmation(parish))
                .replace("${parishId}", parish)
                .replace("${massId}", mass)
                .replace("${lastModifiedDate}", checkedModified(data))));
    }
}
