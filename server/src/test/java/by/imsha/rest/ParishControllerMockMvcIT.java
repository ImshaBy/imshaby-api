package by.imsha.rest;

import by.imsha.Application;
import by.imsha.domain.Parish;
import by.imsha.domain.LocalizedParish;
import by.imsha.domain.Mass;
import by.imsha.domain.City;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Autowired ParishRepository parishes;
    @Autowired MassRepository masses;
    @Autowired CityRepository cities;
    @Autowired CacheManager caches;
    @Autowired ObjectMapper json;
    private String directory;

    @BeforeEach void isolate(TestInfo info) {
        directory = "parish-mockmvc/" + info.getTestMethod().orElseThrow().getName() + "/";
        parishes.deleteAll();
        masses.deleteAll();
        cities.deleteAll();
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        IDENTITY.resetAll();
    }
    private String fixture(String name) throws Exception {
        return new ClassPathResource(directory + name).getContentAsString(StandardCharsets.UTF_8);
    }
    private Parish saved() {
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setCityId("city-1");
        parish.setKey("st-mary");
        parish.setUserId("user-1");
        parish.setState(Parish.State.INITIAL);
        return parishes.save(parish);
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
        assertThat(parishes.findById(id).orElseThrow().getState()).isEqualTo(Parish.State.INITIAL);
        assertThat(parishes.findById(id).orElseThrow().getLastConfirmRelevance()).isNotNull();
    }

    @Test void missingNameIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.count()).isZero();
    }

    @Test void getExistingAndMissingParish() throws Exception {
        Parish parish = saved();
        MvcResult found = mvc.perform(get(URL + "/" + parish.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(found.getResponse().getContentAsString());
        assertThat(body.path("id").asText()).isEqualTo(parish.getId());
        assertThat(body.path("name").asText()).isEqualTo("St. Mary");
        mvc.perform(get(URL + "/000000000000000000000000").header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
    }

    @Test void getByUserFindsParish() throws Exception {
        Parish parish = saved();
        MvcResult result = mvc.perform(get(URL + "/user/user-1").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString()).path("id").asText())
                .isEqualTo(parish.getId());
        mvc.perform(get(URL + "/user/missing").header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
    }

    @Test void getAndUpdateState() throws Exception {
        Parish parish = saved();
        assertJson(mvc.perform(get(URL + "/" + parish.getId() + "/state"))
                .andExpect(status().isOk()).andReturn(), parish.getId());
        MvcResult updated = mvc.perform(put(URL + "/" + parish.getId() + "/state")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(updated.getResponse().getContentAsString()).path("status").asText())
                .isEqualTo("UPDATED");
        assertThat(parishes.findById(parish.getId()).orElseThrow().getState()).isEqualTo(Parish.State.APPROVED);
    }

    @Test void missingStateIsRejectedWithoutWrite() throws Exception {
        Parish parish = saved();
        mvc.perform(put(URL + "/" + parish.getId() + "/state")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.findById(parish.getId()).orElseThrow().getState()).isEqualTo(Parish.State.INITIAL);
    }

    @Test void anonymousCannotCreateOrDelete() throws Exception {
        Parish parish = saved();
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete(URL + "/" + parish.getId())).andExpect(status().isUnauthorized());
        assertThat(parishes.count()).isEqualTo(1);
    }

    @Test void stateWriteRequiresAuthentication() throws Exception {
        Parish parish = saved();
        mvc.perform(put(URL + "/" + parish.getId() + "/state")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"APPROVED\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(parishes.findById(parish.getId()).orElseThrow().getState()).isEqualTo(Parish.State.INITIAL);
    }

    @Test void updateChangesProvidedFieldsAndKeepsOthers() throws Exception {
        Parish parish = saved();
        MvcResult result = mvc.perform(put(URL + "/" + parish.getId()).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        assertJson(result, parish.getId());
        Parish updated = parishes.findById(parish.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("St. Joseph");
        assertThat(updated.getCityId()).isEqualTo("city-1");
        assertThat(updated.getKey()).isEqualTo("st-mary");
    }

    @Test void updateMissingParishDoesNotCreateIt() throws Exception {
        mvc.perform(put(URL + "/000000000000000000000000").header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isNotFound());
        assertThat(parishes.count()).isZero();
    }

    @Test void localizeParishPersistsLanguageAndValues() throws Exception {
        Parish parish = saved();
        MvcResult result = mvc.perform(put(URL + "/" + parish.getId() + "/lang/be")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        assertJson(result, parish.getId());
        assertThat(((LocalizedParish) parishes.findById(parish.getId()).orElseThrow().getLocalizedInfo().get("be")).getName())
                .isEqualTo("Святая Марыя");
    }

    @Test void emptyLocalizedNameIsRejectedWithoutWrite() throws Exception {
        Parish parish = saved();
        mvc.perform(put(URL + "/" + parish.getId() + "/lang/be")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.findById(parish.getId()).orElseThrow().getLocalizedInfo()).isEmpty();
    }

    @Test void deleteParishAndRejectUnknownId() throws Exception {
        Parish parish = saved();
        assertJson(mvc.perform(delete(URL + "/" + parish.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn(), parish.getId());
        assertThat(parishes.findById(parish.getId())).isEmpty();
        mvc.perform(delete(URL + "/000000000000000000000000").header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
    }

    @Test void confirmRelevanceUpdatesStoredTimestamp() throws Exception {
        Parish parish = saved();
        assertThat(parish.getLastConfirmRelevance()).isNull();
        MvcResult result = mvc.perform(post(URL + "/" + parish.getId() + "/confirm-relevance")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertJson(result, parish.getId());
        assertThat(parishes.findById(parish.getId()).orElseThrow().getLastConfirmRelevance()).isNotNull();
    }

    @Test void confirmRelevanceRejectsUnknownId() throws Exception {
        mvc.perform(post(URL + "/000000000000000000000000/confirm-relevance")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
        assertThat(parishes.count()).isZero();
    }

    @Test void cascadeDeleteRemovesRelatedMass() throws Exception {
        Parish parish = saved();
        Mass mass = new Mass();
        mass.setCityId("city-1");
        mass.setParishId(parish.getId());
        mass.setLangCode("be");
        mass.setSingleStartTimestamp(1_700_000_000L);
        mass = masses.save(mass);
        MvcResult result = mvc.perform(delete(URL + "/" + parish.getId()).param("cascade", "true")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("id").asText()).isEqualTo(parish.getId());
        assertThat(body.path("status").asText()).isEqualTo("DELETED");
        assertThat(body.path("relatedEntities").size()).isEqualTo(1);
        assertThat(body.path("relatedEntities").get(0).path("id").asText()).isEqualTo(mass.getId());
        assertThat(masses.findById(mass.getId())).isEmpty();
        assertThat(parishes.findById(parish.getId())).isEmpty();
    }

    @Test void plainDeleteKeepsRelatedMass() throws Exception {
        Parish parish = saved();
        Mass mass = new Mass();
        mass.setCityId("city-1");
        mass.setParishId(parish.getId());
        mass.setLangCode("be");
        mass.setSingleStartTimestamp(1_700_000_000L);
        mass = masses.save(mass);
        mvc.perform(delete(URL + "/" + parish.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isOk());
        assertThat(masses.findById(mass.getId())).isPresent();
        assertThat(parishes.findById(parish.getId())).isEmpty();
    }

    @Test void invalidEmailIsRejectedWithoutWrite() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.count()).isZero();
    }

    @Test void filterReturnsMatchingParish() throws Exception {
        Parish expected = saved();
        Parish other = new Parish();
        other.setName("St. Peter");
        other.setCityId("city-1");
        other.setKey("st-peter");
        other.setState(Parish.State.APPROVED);
        parishes.save(other);
        MvcResult result = mvc.perform(get(URL).param("filter", "key==st-mary")
                        .param("offset", "0").param("limit", "1")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).path("id").asText()).isEqualTo(expected.getId());
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
        assertThat(parishes.count()).isZero();
    }

    @Test void filterPagesContainEachMatchingParishOnce() throws Exception {
        Parish first = saved();
        Parish second = new Parish();
        second.setName("St. Peter");
        second.setCityId("city-1");
        second.setKey("st-peter");
        second.setState(Parish.State.APPROVED);
        second = parishes.save(second);
        Parish outsideCity = new Parish();
        outsideCity.setName("Other city");
        outsideCity.setCityId("city-2");
        outsideCity.setKey("other-city");
        outsideCity.setState(Parish.State.APPROVED);
        parishes.save(outsideCity);

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
        assertThat(pageOne.get(0).path("id").asText()).isEqualTo(first.getId());
        assertThat(pageTwo.get(0).path("id").asText()).isEqualTo(second.getId());
    }

    @Test void missingStateIsRejectedOnCreate() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.count()).isZero();
    }

    @Test void missingCityIsRejectedOnCreate() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.count()).isZero();
    }

    @Test void invalidLastModifiedEmailIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.count()).isZero();
    }

    @Test void invalidLatitudeIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.count()).isZero();
    }

    @Test void duplicateKeyIsRejectedWithoutReplacingParish() throws Exception {
        Parish existing = saved();
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isInternalServerError());
        assertThat(parishes.count()).isEqualTo(1);
        Parish unchanged = parishes.findById(existing.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("St. Mary");
        assertThat(unchanged.getKey()).isEqualTo("st-mary");
    }

    @Test void expiredReturnsEmptyResultWhenThereAreNoParishes() throws Exception {
        MvcResult result = mvc.perform(get(URL + "/expired").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @Test void expiredGroupsPastAndSoonToExpireParishes() throws Exception {
        Parish expired = saved();
        expired.setLastConfirmRelevance(LocalDateTime.now().minusDays(10));
        expired.setUpdatePeriodInDays(1);
        expired = parishes.save(expired);
        Parish almost = new Parish();
        almost.setName("St. Peter");
        almost.setCityId("city-1");
        almost.setKey("st-peter");
        almost.setState(Parish.State.APPROVED);
        almost.setLastConfirmRelevance(LocalDateTime.now().minusDays(2));
        almost.setUpdatePeriodInDays(3);
        almost = parishes.save(almost);
        MvcResult result = mvc.perform(get(URL + "/expired").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.path("expiredParishes").size()).isEqualTo(1);
        assertThat(body.path("almostExpiredParishes").size()).isEqualTo(1);
        assertThat(body.path("expiredParishes").get(0).path("id").asText()).isEqualTo(expired.getId());
        assertThat(body.path("almostExpiredParishes").get(0).path("id").asText()).isEqualTo(almost.getId());
        assertThat(body.path("expiredParishes").get(0).path("key").asText()).isEqualTo("st-mary");
        assertThat(body.path("almostExpiredParishes").get(0).path("key").asText()).isEqualTo("st-peter");
    }

    @Test void invalidLongitudeIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.count()).isZero();
    }

    @Test void malformedDateIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.count()).isZero();
    }

    @Test void keyIsTrimmedOnCreate() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isCreated()).andReturn();
        String id = json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        assertThat(parishes.findById(id).orElseThrow().getKey()).isEqualTo("st-mary");
    }

    @Test void unavailableLocaleIsRejectedWithoutWrite() throws Exception {
        Parish parish = saved();
        mvc.perform(put(URL + "/" + parish.getId() + "/lang/zz")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(parishes.findById(parish.getId()).orElseThrow().getLocalizedInfo()).isEmpty();
    }

    @Test void weekExpiredReturnsApprovedParishWithStaleMass() throws Exception {
        City city = cities.save(City.builder().name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setKey("st-mary");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish.setUpdatePeriodInDays(0);
        parish.setLastConfirmRelevance(LocalDateTime.of(2018, 6, 20, 0, 0));
        parish = parishes.save(parish);
        Mass mass = new Mass();
        mass.setCityId(city.getId());
        mass.setParishId(parish.getId());
        mass.setLangCode("be");
        mass.setTime("08:00");
        mass.setDays(new int[]{1});
        masses.save(mass);
        MvcResult result = mvc.perform(get(URL + "/week/expired").param("date", "23-06-2018")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", parish.getId())));
    }

    @Test void weekExpiredDefaultsToToday() throws Exception {
        City city = cities.save(City.builder().name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setKey("st-mary");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish.setUpdatePeriodInDays(0);
        parish.setLastConfirmRelevance(LocalDateTime.of(2023, 7, 12, 0, 0));
        parish = parishes.save(parish);
        Mass mass = new Mass();
        mass.setCityId(city.getId());
        mass.setParishId(parish.getId());
        mass.setLangCode("be");
        mass.setTime("08:00");
        mass.setDays(new int[]{7});
        masses.save(mass);
        MvcResult result = mvc.perform(get(URL + "/week/expired").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", parish.getId())));
    }

    @Test void cachedWeekExpiredAfterBulkMassDeleteIsEmpty() throws Exception {
        City city = cities.save(City.builder().name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setKey("st-mary");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish.setUpdatePeriodInDays(0);
        parish.setLastConfirmRelevance(LocalDateTime.of(2018, 6, 20, 0, 0));
        parish = parishes.save(parish);
        Mass mass = new Mass();
        mass.setCityId(city.getId());
        mass.setParishId(parish.getId());
        mass.setLangCode("be");
        mass.setTime("08:00");
        mass.setDays(new int[]{1});
        mass = masses.save(mass);
        MvcResult first = mvc.perform(get(URL + "/week/expired").param("date", "23-06-2018")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        JsonNode expected = json.readTree(fixture("response.json").replace("${id}", parish.getId()));
        assertThat(json.readTree(first.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(expected);
        MvcResult deletion = mvc.perform(delete("/api/mass").param("parishId", parish.getId())
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(deletion.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("delete-response.json").replace("${massId}", mass.getId())));
        assertThat(masses.findById(mass.getId())).isEmpty();
        MvcResult after = mvc.perform(get(URL + "/week/expired").param("date", "23-06-2018")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(after.getResponse().getContentAsString(StandardCharsets.UTF_8)).isEmpty()).isTrue();
    }

    @Test void weekExpiredExcludesPendingAndDisabledParishes() throws Exception {
        City city = cities.save(City.builder().name("Minsk").key("minsk").build());
        for (Parish.State state : new Parish.State[]{Parish.State.PENDING, Parish.State.DISABLED}) {
            Parish parish = new Parish();
            parish.setName(state.name());
            parish.setKey(state.name().toLowerCase());
            parish.setCityId(city.getId());
            parish.setState(state);
            parish.setUpdatePeriodInDays(0);
            parish.setLastConfirmRelevance(LocalDateTime.of(2018, 6, 20, 0, 0));
            parish = parishes.save(parish);
            Mass mass = new Mass();
            mass.setCityId(city.getId());
            mass.setParishId(parish.getId());
            mass.setLangCode("be");
            mass.setTime("08:00");
            mass.setDays(new int[]{1});
            masses.save(mass);
        }
        MvcResult result = mvc.perform(get(URL + "/week/expired").param("date", "23-06-2018")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
        assertThat(parishes.count()).isEqualTo(2);
        assertThat(masses.count()).isEqualTo(2);
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
