package by.imsha.rest;

import by.imsha.Application;
import by.imsha.utils.DateTimeProvider;
import by.imsha.repository.CityRepository;
import by.imsha.repository.MassRepository;
import by.imsha.repository.ParishRepository;
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
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
class ParishControllerMockMvcIT {
    private static final String URL = "/api/parish";
    private static final String KEY = "TEST_API_KEY";
    @Container static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
    static final WireMockServer IDENTITY = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    static { IDENTITY.start(); }
    @AfterAll static void stopStub() { IDENTITY.stop(); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_parish_mockmvc");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
    }

    @Autowired MockMvc mvc;
    @MockitoSpyBean DateTimeProvider dateTimeProvider;
    @Autowired ParishRepository parishes;
    @Autowired MassRepository masses;
    @Autowired CityRepository cities;
    @Autowired CacheManager caches;
    @Autowired ObjectMapper json;
    private V1TestApi api;
    private String directory;

    @BeforeEach void isolate(TestInfo info) {
        directory = "parish-mockmvc/" + info.getTestMethod().orElseThrow().getName() + "/";
        api = new V1TestApi(mvc, json);
        parishes.deleteAll();
        masses.deleteAll();
        cities.deleteAll();
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        IDENTITY.resetAll();
        when(dateTimeProvider.nowSystemDefaultZone()).thenReturn(LocalDateTime.of(2023, 7, 16, 16, 43, 1));
    }
    private String fixture(String name) throws Exception {
        return new ClassPathResource(directory + name).getContentAsString(StandardCharsets.UTF_8);
    }
    private String saved() throws Exception {
        return api.parish("INITIAL", Map.of(
                "name", "St. Mary",
                "cityId", "city-1",
                "key", "st-mary",
                "userId", "user-1"));
    }
    private void assertJson(MvcResult result, String id) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", id)));
    }

    @Test void createOverridesRequestedState() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        String id = body.path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        assertThat(body.path("state").asText()).isEqualTo("INITIAL");
        assertThat(api.getEntity(V1TestApi.PARISHES, id).path("state").asText()).isEqualTo("INITIAL");
        assertThat(api.getEntity(V1TestApi.PARISHES, id).path("lastConfirmRelevance").asText()).isNotBlank();
    }

    @Test void missingNameIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void getExistingAndMissingParish() throws Exception {
        String parish = saved();
        MvcResult found = mvc.perform(get(URL + "/" + parish).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(found.getResponse().getContentAsString());
        assertThat(body.path("id").asText()).isEqualTo(parish);
        assertThat(body.path("name").asText()).isEqualTo("St. Mary");
        mvc.perform(get(URL + "/000000000000000000000000").header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
    }

    @Test void getByUserFindsParish() throws Exception {
        String parish = saved();
        MvcResult result = mvc.perform(get(URL + "/user/user-1").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString()).path("id").asText())
                .isEqualTo(parish);
        mvc.perform(get(URL + "/user/missing").header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
    }

    @Test void getAndUpdateState() throws Exception {
        String parish = saved();
        assertJson(mvc.perform(get(URL + "/" + parish + "/state"))
                .andExpect(status().isOk()).andReturn(), parish);
        MvcResult updated = mvc.perform(put(URL + "/" + parish + "/state")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(updated.getResponse().getContentAsString()).path("status").asText())
                .isEqualTo("UPDATED");
        assertThat(api.getEntity(V1TestApi.PARISHES, parish).path("state").asText()).isEqualTo("APPROVED");
    }

    @Test void missingStateIsRejectedWithoutWrite() throws Exception {
        String parish = saved();
        mvc.perform(put(URL + "/" + parish + "/state")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.getEntity(V1TestApi.PARISHES, parish).path("state").asText()).isEqualTo("INITIAL");
    }

    @Test void anonymousCannotCreateOrDelete() throws Exception {
        String parish = saved();
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete(URL + "/" + parish)).andExpect(status().isUnauthorized());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isEqualTo(1);
    }

    @Test void stateWriteRequiresAuthentication() throws Exception {
        String parish = saved();
        mvc.perform(put(URL + "/" + parish + "/state")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"APPROVED\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(api.getEntity(V1TestApi.PARISHES, parish).path("state").asText()).isEqualTo("INITIAL");
    }

    @Test void updateChangesProvidedFieldsAndKeepsOthers() throws Exception {
        String parish = saved();
        MvcResult result = mvc.perform(put(URL + "/" + parish).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        assertJson(result, parish);
        JsonNode updated = api.getEntity(V1TestApi.PARISHES, parish);
        assertThat(updated.path("name").asText()).isEqualTo("St. Joseph");
        assertThat(updated.path("cityId").asText()).isEqualTo("city-1");
        assertThat(updated.path("key").asText()).isEqualTo("st-mary");
    }

    @Test void updateMissingParishDoesNotCreateIt() throws Exception {
        mvc.perform(put(URL + "/000000000000000000000000").header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isNotFound());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void localizeParishPersistsLanguageAndValues() throws Exception {
        String parish = saved();
        MvcResult result = mvc.perform(put(URL + "/" + parish + "/lang/be")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        assertJson(result, parish);
        assertThat(api.getEntity(V1TestApi.PARISHES, parish).path("localizedInfo").path("be").path("name").asText())
                .isEqualTo("Святая Марыя");
    }

    @Test void emptyLocalizedNameIsRejectedWithoutWrite() throws Exception {
        String parish = saved();
        mvc.perform(put(URL + "/" + parish + "/lang/be")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.getEntity(URL, parish).path("localizedInfo")).isEqualTo(json.readTree("{}"));
    }

    @Test void deleteParishAndRejectUnknownId() throws Exception {
        String parish = saved();
        assertJson(mvc.perform(delete(URL + "/" + parish).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn(), parish);
        api.expectMissing(V1TestApi.PARISHES, parish);
        mvc.perform(delete(URL + "/000000000000000000000000").header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
    }

    @Test void confirmRelevanceUpdatesStoredTimestamp() throws Exception {
        // POST /api/parish initializes this timestamp, so confirm against a known earlier value.
        String parish = saved();
        assertThat(api.getEntity(URL, parish).path("lastConfirmRelevance").asText()).isEqualTo("16-07-2023 16:43:01");
        when(dateTimeProvider.nowSystemDefaultZone()).thenReturn(LocalDateTime.of(2023, 7, 17, 10, 0));
        MvcResult result = mvc.perform(post(URL + "/" + parish + "/confirm-relevance")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertJson(result, parish);
        assertThat(api.getEntity(V1TestApi.PARISHES, parish).path("lastConfirmRelevance").asText()).isEqualTo("17-07-2023 10:00:00");
    }

    @Test void confirmRelevanceRejectsUnknownId() throws Exception {
        mvc.perform(post(URL + "/000000000000000000000000/confirm-relevance")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void cascadeDeleteRemovesRelatedMass() throws Exception {
        String parish = saved();
        String mass = api.create(V1TestApi.MASSES, Map.of(
                "cityId", "city-1",
                "parishId", parish,
                "langCode", "be",
                "singleStartTimestamp", 1_700_000_000L));
        MvcResult result = mvc.perform(delete(URL + "/" + parish).param("cascade", "true")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("id").asText()).isEqualTo(parish);
        assertThat(body.path("status").asText()).isEqualTo("DELETED");
        assertThat(body.path("relatedEntities").size()).isEqualTo(1);
        assertThat(body.path("relatedEntities").get(0).path("id").asText()).isEqualTo(mass);
        api.expectMissing(V1TestApi.MASSES, mass);
        api.expectMissing(V1TestApi.PARISHES, parish);
    }

    @Test void plainDeleteKeepsRelatedMass() throws Exception {
        String parish = saved();
        String mass = api.create(V1TestApi.MASSES, Map.of(
                "cityId", "city-1",
                "parishId", parish,
                "langCode", "be",
                "singleStartTimestamp", 1_700_000_000L));
        mvc.perform(delete(URL + "/" + parish).header("X-Api-Key", KEY))
                .andExpect(status().isOk());
        api.getEntity(V1TestApi.MASSES, mass);
        api.expectMissing(V1TestApi.PARISHES, parish);
    }

    @Test void invalidEmailIsRejectedWithoutWrite() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void filterReturnsMatchingParish() throws Exception {
        String expected = saved();
        String other = api.parish("APPROVED", Map.of(
                "name", "St. Peter",
                "cityId", "city-1",
                "key", "st-peter"));
        MvcResult result = mvc.perform(get(URL).param("filter", "key==st-mary")
                        .param("offset", "0").param("limit", "1")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).path("id").asText()).isEqualTo(expected);
    }

    @Test void filterRequiresExpression() throws Exception {
        MvcResult result = mvc.perform(get(URL).header("X-Api-Key", KEY))
                .andExpect(status().isBadRequest()).andReturn();
        assertJson(result, "");
    }

    @Test void malformedFilterIsRejected() throws Exception {
        MvcResult result = mvc.perform(get(URL).param("filter", "state===PENDING")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString()).path("errors").isEmpty()).isTrue();
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void filterPagesContainEachMatchingParishOnce() throws Exception {
        String first = saved();
        String second = api.parish("APPROVED", Map.of(
                "name", "St. Peter",
                "cityId", "city-1",
                "key", "st-peter"));
        String outsideCity = api.parish("APPROVED", Map.of(
                "name", "Other city",
                "cityId", "city-2",
                "key", "other-city"));

        JsonNode pageOne = json.readTree(mvc.perform(get(URL).param("filter", "cityId==city-1")
                        .param("offset", "0").param("limit", "1").param("sort", "+name")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode pageTwo = json.readTree(mvc.perform(get(URL).param("filter", "cityId==city-1")
                        .param("offset", "1").param("limit", "1").param("sort", "+name")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode pageThree = json.readTree(mvc.perform(get(URL).param("filter", "cityId==city-1")
                        .param("offset", "2").param("limit", "1").param("sort", "+name")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(pageOne.size()).isEqualTo(1);
        assertThat(pageTwo.size()).isEqualTo(1);
        assertThat(pageThree.size()).isZero();
        assertThat(pageOne.get(0).path("id").asText()).isEqualTo(first);
        assertThat(pageTwo.get(0).path("id").asText()).isEqualTo(second);
    }

    @Test void missingStateIsRejectedOnCreate() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void missingCityIsRejectedOnCreate() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void invalidLastModifiedEmailIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void invalidLatitudeIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void duplicateKeyIsRejectedWithoutReplacingParish() throws Exception {
        String existing = saved();
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isInternalServerError());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isEqualTo(1);
        JsonNode unchanged = api.getEntity(V1TestApi.PARISHES, existing);
        assertThat(unchanged.path("name").asText()).isEqualTo("St. Mary");
        assertThat(unchanged.path("key").asText()).isEqualTo("st-mary");
    }

    @Test void expiredReturnsEmptyResultWhenThereAreNoParishes() throws Exception {
        MvcResult result = mvc.perform(get(URL + "/expired").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test void expiredGroupsPastAndSoonToExpireParishes() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        when(dateTimeProvider.nowSystemDefaultZone()).thenReturn(now.minusDays(10));
        String expired = api.parish("INITIAL", Map.of("name", "St. Mary", "cityId", "city-1",
                "key", "st-mary", "updatePeriodInDays", 1));
        when(dateTimeProvider.nowSystemDefaultZone()).thenReturn(now.minusDays(2));
        String almost = api.parish("APPROVED", Map.of(
                "name", "St. Peter",
                "cityId", "city-1",
                "key", "st-peter",
                "updatePeriodInDays", 3));
        MvcResult result = mvc.perform(get(URL + "/expired").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.path("expiredParishes").size()).isEqualTo(1);
        assertThat(body.path("almostExpiredParishes").size()).isEqualTo(1);
        assertThat(body.path("expiredParishes").get(0).path("id").asText()).isEqualTo(expired);
        assertThat(body.path("almostExpiredParishes").get(0).path("id").asText()).isEqualTo(almost);
        assertThat(body.path("expiredParishes").get(0).path("key").asText()).isEqualTo("st-mary");
        assertThat(body.path("almostExpiredParishes").get(0).path("key").asText()).isEqualTo("st-peter");
    }

    @Test void invalidLongitudeIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void malformedDateIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isZero();
    }

    @Test void keyIsTrimmedOnCreate() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isCreated()).andReturn();
        String id = json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        assertThat(api.getEntity(V1TestApi.PARISHES, id).path("key").asText()).isEqualTo("st-mary");
    }

    @Test void unavailableLocaleIsRejectedWithoutWrite() throws Exception {
        String parish = saved();
        mvc.perform(put(URL + "/" + parish + "/lang/zz")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.getEntity(URL, parish).path("localizedInfo")).isEqualTo(json.readTree("{}"));
    }

    @Test void weekExpiredReturnsApprovedParishWithStaleMass() throws Exception {
        String city = api.city("minsk", "Minsk");
        when(dateTimeProvider.nowSystemDefaultZone()).thenReturn(LocalDateTime.of(2018, 6, 20, 0, 0));
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "key", "st-mary",
                "cityId", city,
                "updatePeriodInDays", 0));
        String mass = api.create(V1TestApi.MASSES, Map.of(
                "cityId", city,
                "parishId", parish,
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1}));
        MvcResult result = mvc.perform(get(URL + "/week/expired").param("date", "23-06-2018")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", parish)));
    }

    @Test void weekExpiredDefaultsToToday() throws Exception {
        String city = api.city("minsk", "Minsk");
        when(dateTimeProvider.nowSystemDefaultZone()).thenReturn(LocalDateTime.of(2023, 7, 12, 0, 0));
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "key", "st-mary",
                "cityId", city,
                "updatePeriodInDays", 0));
        String mass = api.create(V1TestApi.MASSES, Map.of(
                "cityId", city,
                "parishId", parish,
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{7}));
        MvcResult result = mvc.perform(get(URL + "/week/expired").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", parish)));
    }

    @Test void cachedWeekExpiredAfterBulkMassDeleteIsEmpty() throws Exception {
        String city = api.city("minsk", "Minsk");
        when(dateTimeProvider.nowSystemDefaultZone()).thenReturn(LocalDateTime.of(2018, 6, 20, 0, 0));
        String parish = api.parish("APPROVED", Map.of(
                "name", "St. Mary",
                "key", "st-mary",
                "cityId", city,
                "updatePeriodInDays", 0));
        String mass = api.create(V1TestApi.MASSES, Map.of(
                "cityId", city,
                "parishId", parish,
                "langCode", "be",
                "time", "08:00",
                "days", new int[]{1}));
        MvcResult first = mvc.perform(get(URL + "/week/expired").param("date", "23-06-2018")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        JsonNode expected = json.readTree(fixture("response.json").replace("${id}", parish));
        assertThat(json.readTree(first.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(expected);
        MvcResult deletion = mvc.perform(delete("/api/mass").param("parishId", parish)
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(deletion.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("delete-response.json").replace("${massId}", mass)));
        api.expectMissing(V1TestApi.MASSES, mass);
        MvcResult after = mvc.perform(get(URL + "/week/expired").param("date", "23-06-2018")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(after.getResponse().getContentAsString(StandardCharsets.UTF_8)).isEmpty()).isTrue();
    }

    @Test void weekExpiredExcludesPendingAndDisabledParishes() throws Exception {
        String city = api.city("minsk", "Minsk");
        for (String state : List.of("PENDING", "DISABLED")) {
            when(dateTimeProvider.nowSystemDefaultZone()).thenReturn(LocalDateTime.of(2018, 6, 20, 0, 0));
            String parish = api.parish(state, Map.of(
                    "name", state,
                    "key", state.toLowerCase(),
                    "cityId", city,
                    "updatePeriodInDays", 0));
            String mass = api.create(V1TestApi.MASSES, Map.of(
                    "cityId", city,
                    "parishId", parish,
                    "langCode", "be",
                    "time", "08:00",
                    "days", new int[]{1}));
        }
        MvcResult result = mvc.perform(get(URL + "/week/expired").param("date", "23-06-2018")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
        assertThat(api.filtered(V1TestApi.PARISHES).size()).isEqualTo(2);
        assertThat(api.filtered(V1TestApi.MASSES).size()).isEqualTo(2);
    }

    @Test void weekExpiredRejectsMalformedDate() throws Exception {
        mvc.perform(get(URL + "/week/expired").param("date", "not-a-date")
                        .header("X-Api-Key", KEY)).andExpect(status().isBadRequest());
    }

    @Test void anonymousCannotReadWeekExpired() throws Exception {
        mvc.perform(get(URL + "/week/expired").param("date", "23-06-2018"))
                .andExpect(status().isUnauthorized());
    }
}
