package by.imsha.rest;

import by.imsha.Application;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.City;
import by.imsha.repository.MassRepository;
import by.imsha.repository.ParishRepository;
import by.imsha.repository.CityRepository;
import by.imsha.meilisearch.reader.MeilisearchReader;
import by.imsha.meilisearch.reader.SearchResult;
import by.imsha.meilisearch.reader.MassSearchFilter;
import by.imsha.meilisearch.model.SearchResultItem;
import by.imsha.meilisearch.model.Geo;
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
import org.springframework.cache.Cache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;
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
    @Autowired MassRepository masses;
    @Autowired ParishRepository parishes;
    @Autowired CityRepository cities;
    @Autowired CacheManager caches;
    @Autowired ObjectMapper json;
    @MockitoBean MeilisearchReader meilisearchReader;
    private String directory;
    @BeforeEach void isolate(TestInfo info) {
        directory = "mass-mockmvc/" + info.getTestMethod().orElseThrow().getName() + "/";
        masses.deleteAll();
        parishes.deleteAll();
        cities.deleteAll();
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        IDENTITY.resetAll();
    }
    private String fixture(String name) throws Exception {
        return new ClassPathResource(directory + name).getContentAsString(StandardCharsets.UTF_8);
    }
    private Mass saved(String parishId) {
        Mass mass = new Mass();
        mass.setCityId("city-1");
        mass.setParishId(parishId);
        mass.setLangCode("be");
        mass.setTime("08:00");
        mass.setDays(new int[]{1});
        return masses.save(mass);
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
        assertThat(masses.count()).isZero();
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
        assertThat(masses.findById(id).orElseThrow().getDays()).containsExactly(1);
    }

    @Test void missingCityIsRejectedWithoutWrite() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(masses.count()).isZero();
    }

    @Test void getExistingAndMissingMass() throws Exception {
        Mass mass = saved("parish-1");
        JsonNode body = json.readTree(mvc.perform(get(URL + "/" + mass.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(body.path("id").asText()).isEqualTo(mass.getId());
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${id}", mass.getId())
                .replace("${lastModifiedDate}", checkedModified(body))));
        mvc.perform(get(URL + "/000000000000000000000000").header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
    }

    @Test void replaceMassUpdatesStoredTime() throws Exception {
        Mass mass = saved("parish-1");
        assertJson(mvc.perform(put(URL + "/" + mass.getId()).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn(), mass.getId());
        assertThat(masses.findById(mass.getId()).orElseThrow().getTime()).isEqualTo("09:00");
    }

    @Test void refreshMassChangesOnlyProvidedField() throws Exception {
        Mass mass = saved("parish-1");
        assertJson(mvc.perform(put(URL + "/refresh/" + mass.getId()).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn(), mass.getId());
        Mass updated = masses.findById(mass.getId()).orElseThrow();
        assertThat(updated.getTime()).isEqualTo("09:00");
        assertThat(updated.getCityId()).isEqualTo("city-1");
        assertThat(updated.getParishId()).isEqualTo("parish-1");
    }

    @Test void deleteMassRemovesIt() throws Exception {
        Mass mass = saved("parish-1");
        assertJson(mvc.perform(delete(URL + "/" + mass.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn(), mass.getId());
        assertThat(masses.findById(mass.getId())).isEmpty();
    }

    @Test void missingMassCannotBeUpdatedOrDeleted() throws Exception {
        String id = "000000000000000000000000";
        mvc.perform(put(URL + "/" + id).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isNotFound());
        mvc.perform(delete(URL + "/" + id).header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
        assertThat(masses.count()).isZero();
    }

    @Test void invalidRemovalIntervalPreservesMass() throws Exception {
        Mass mass = saved("parish-1");
        mvc.perform(delete(URL + "/" + mass.getId()).param("from", "02-01-2025")
                        .param("to", "01-01-2025").header("X-Api-Key", KEY))
                .andExpect(status().isBadRequest());
        assertThat(masses.findById(mass.getId())).isPresent();
    }

    @Test void removalFromDateWithoutToTruncatesPeriodicMass() throws Exception {
        Mass mass = saved("parish-1");
        mass.setStartDate(LocalDate.of(2025, 1, 1));
        mass.setEndDate(LocalDate.of(2025, 1, 31));
        mass = masses.save(mass);
        MvcResult result = mvc.perform(delete(URL + "/" + mass.getId()).param("from", "08-01-2025")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", mass.getId())));
        Mass remaining = masses.findById(mass.getId()).orElseThrow();
        assertThat(remaining.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(remaining.getEndDate()).isEqualTo(LocalDate.of(2025, 1, 7));
        assertThat(remaining.getDays()).containsExactly(1);
        assertThat(masses.count()).isEqualTo(1);
    }

    @Test void removeAllMassesByParish() throws Exception {
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setCityId("city-1");
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass first = saved(parish.getId());
        MvcResult result = mvc.perform(delete(URL).param("parishId", parish.getId())
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("status").asText()).isEqualTo("DELETED");
        assertThat(body.path("entities").get(0).asText()).isEqualTo(first.getId());
        assertThat(masses.count()).isZero();
    }

    @Test void cachedMassReadAfterBulkDeleteReturnsNotFound() throws Exception {
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setCityId("city-1");
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass mass = saved(parish.getId());
        JsonNode first = json.readTree(mvc.perform(get(URL + "/" + mass.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(first.path("id").asText()).isEqualTo(mass.getId());
        JsonNode expected = json.readTree(fixture("response.json")
                .replace("${id}", mass.getId())
                .replace("${parishId}", parish.getId())
                .replace("${lastModifiedDate}", checkedModified(first)));
        assertThat(first).isEqualTo(expected);
        MvcResult deletion = mvc.perform(delete(URL).param("parishId", parish.getId())
                .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(deletion.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("entities").get(0).asText()).isEqualTo(mass.getId());
        assertThat(masses.findById(mass.getId())).isEmpty();
        mvc.perform(get(URL + "/" + mass.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
    }

    @Test void anonymousCannotCreateMass() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isUnauthorized());
        assertThat(masses.count()).isZero();
    }

    @Test void anonymousCannotDeleteMass() throws Exception {
        Mass mass = saved("parish-1");
        mvc.perform(delete(URL + "/" + mass.getId())).andExpect(status().isUnauthorized());
        assertThat(masses.findById(mass.getId())).isPresent();
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
        Mass stored = masses.findById(id).orElseThrow();
        assertThat(stored.getSingleStartTimestamp()).isEqualTo(1_700_000_000L);
        assertThat(stored.getTime()).isNull();
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
        assertThat(masses.count()).isZero();
    }

    @Test void invalidPeriodicTimeIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(masses.count()).isZero();
    }

    @Test void missingPeriodicDaysAreRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(masses.count()).isZero();
    }

    @Test void refreshMissingMassReturns404() throws Exception {
        mvc.perform(put(URL + "/refresh/000000000000000000000000")
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isNotFound());
        assertThat(masses.count()).isZero();
    }

    @Test void refreshWithoutBodyKeepsMass() throws Exception {
        Mass mass = saved("parish-1");
        mvc.perform(put(URL + "/refresh/" + mass.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isOk());
        Mass updated = masses.findById(mass.getId()).orElseThrow();
        assertThat(updated.getTime()).isEqualTo("08:00");
        assertThat(updated.getDays()).containsExactly(1);
    }

    @Test void bulkRefreshUpdatesParishTimestamp() throws Exception {
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setCityId("city-1");
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        MvcResult result = mvc.perform(put(URL).param("parishId", parish.getId())
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("entities").get(0).asText()).isEqualTo(parish.getId());
        assertThat(body.path("status").asText()).isEqualTo("UPDATED");
        assertThat(parishes.findById(parish.getId()).orElseThrow().getLastConfirmRelevance()).isNotNull();
    }

    @Test void bulkDeleteRejectsUnknownParish() throws Exception {
        mvc.perform(delete(URL).param("parishId", "000000000000000000000000")
                        .header("X-Api-Key", KEY)).andExpect(status().isNotFound());
        assertThat(masses.count()).isZero();
    }

    @Test void deleteWholePeriodicMassThroughInterval() throws Exception {
        Mass mass = saved("parish-1");
        mass.setStartDate(LocalDate.of(2025, 1, 1));
        mass.setEndDate(LocalDate.of(2025, 1, 31));
        mass = masses.save(mass);
        MvcResult result = mvc.perform(delete(URL + "/" + mass.getId())
                        .param("from", "31-12-2024").param("to", "01-02-2025")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).path("status").asText()).isEqualTo("DELETED");
        assertThat(body.get(0).path("id").asText()).isEqualTo(mass.getId());
        assertThat(masses.findById(mass.getId())).isEmpty();
    }

    @Test void removeMiddleOfPeriodicMassSplitsStoredRange() throws Exception {
        Mass original = saved("parish-1");
        original.setStartDate(LocalDate.of(2025, 1, 1));
        original.setEndDate(LocalDate.of(2025, 1, 31));
        original = masses.save(original);
        MvcResult result = mvc.perform(delete(URL + "/" + original.getId())
                        .param("from", "08-01-2025").param("to", "14-01-2025")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.size()).isEqualTo(2);
        assertThat(body.get(0).path("id").asText()).isEqualTo(original.getId());
        assertThat(body.get(0).path("status").asText()).isEqualTo("UPDATED");
        String createdId = body.get(1).path("id").asText();
        assertThat(createdId).matches("[0-9a-f]{24}").isNotEqualTo(original.getId());
        assertThat(body.get(1).path("status").asText()).isEqualTo("CREATED");
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")
                        .replace("${originalId}", original.getId()).replace("${createdId}", createdId)));
        assertThat(masses.count()).isEqualTo(2);
        Mass before = masses.findById(original.getId()).orElseThrow();
        Mass after = masses.findById(createdId).orElseThrow();
        assertThat(before.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(before.getEndDate()).isEqualTo(LocalDate.of(2025, 1, 7));
        assertThat(after.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(after.getEndDate()).isEqualTo(LocalDate.of(2025, 1, 31));
        assertThat(before.getDays()).containsExactly(1);
        assertThat(after.getDays()).containsExactly(1);
    }

    @Test void removeStartOfPeriodicMassKeepsOriginalId() throws Exception {
        Mass mass = saved("parish-1");
        mass.setStartDate(LocalDate.of(2025, 1, 1));
        mass.setEndDate(LocalDate.of(2025, 1, 31));
        mass = masses.save(mass);
        MvcResult result = mvc.perform(delete(URL + "/" + mass.getId()).param("from", "01-01-2025")
                        .param("to", "07-01-2025").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).path("id").asText()).isEqualTo(mass.getId());
        assertThat(body.get(0).path("status").asText()).isEqualTo("UPDATED");
        assertThat(masses.count()).isEqualTo(1);
        Mass remaining = masses.findById(mass.getId()).orElseThrow();
        assertThat(remaining.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 8));
        assertThat(remaining.getEndDate()).isEqualTo(LocalDate.of(2025, 1, 31));
        assertThat(remaining.getDays()).containsExactly(1);
    }

    @Test void removeOnlyWeekdayDeletesShortPeriodicRemainder() throws Exception {
        Mass mass = saved("parish-1");
        mass.setStartDate(LocalDate.of(2025, 1, 6));
        mass.setEndDate(LocalDate.of(2025, 1, 8));
        mass = masses.save(mass);

        MvcResult result = mvc.perform(delete(URL + "/" + mass.getId())
                        .param("from", "06-01-2025").param("to", "06-01-2025")
                        .header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();

        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", mass.getId())));
        assertThat(masses.findById(mass.getId())).isEmpty();
    }

    @Test void removeEndOfPeriodicMassKeepsOriginalId() throws Exception {
        Mass mass = saved("parish-1");
        mass.setStartDate(LocalDate.of(2025, 1, 1));
        mass.setEndDate(LocalDate.of(2025, 1, 31));
        mass = masses.save(mass);
        MvcResult result = mvc.perform(delete(URL + "/" + mass.getId()).param("from", "25-01-2025")
                        .param("to", "31-01-2025").header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).path("id").asText()).isEqualTo(mass.getId());
        assertThat(body.get(0).path("status").asText()).isEqualTo("UPDATED");
        assertThat(masses.count()).isEqualTo(1);
        Mass remaining = masses.findById(mass.getId()).orElseThrow();
        assertThat(remaining.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(remaining.getEndDate()).isEqualTo(LocalDate.of(2025, 1, 24));
        assertThat(remaining.getDays()).containsExactly(1);
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
        Mass existing = saved("parish-1");
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(masses.count()).isEqualTo(1);
        assertThat(masses.findById(existing.getId())).isPresent();
    }

    @Test void invalidWeekdayIsRejected() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(masses.count()).isZero();
    }

    @Test void filterReturnsMatchingMass() throws Exception {
        Mass expected = saved("parish-1");
        Mass other = new Mass();
        other.setCityId("city-1");
        other.setParishId("parish-2");
        other.setLangCode("be");
        other.setTime("09:00");
        other.setDays(new int[]{1});
        masses.save(other);
        MvcResult result = mvc.perform(get(URL).param("filter", "parishId==parish-1")
                        .param("offset", "0").param("limit", "1")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.size()).isEqualTo(1);
        assertThat(body.get(0).path("id").asText()).isEqualTo(expected.getId());
    }

    @Test void mapReturnsIndexedCoordinates() throws Exception {
        when(meilisearchReader.searchNearestMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of(indexedMass("city-1", "parish-1")))
                        .facetDistribution(Map.of()).build());
        MvcResult result = mvc.perform(get(URL + "/map")
                        .param("dateTimeFrom", "2024-06-24T00:00:00+03:00"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
        ArgumentCaptor<MassSearchFilter> filter = ArgumentCaptor.forClass(MassSearchFilter.class);
        verify(meilisearchReader).searchNearestMasses(filter.capture());
        assertThat(filter.getValue().dateTimeFrom()).isEqualTo(OffsetDateTime.parse("2024-06-24T00:00:00+03:00"));
        assertThat(filter.getValue().dateTimeTo().toLocalDate()).isEqualTo(LocalDate.of(2024, 6, 24));
    }

    @Test void mapReturnsUnapprovedParishes() throws Exception {
        cities.save(City.builder().id("city-1").name("Minsk").key("minsk").build());
        Parish pending = new Parish();
        pending.setId("pending-parish");
        pending.setName("Pending parish");
        pending.setAddress("Church street 1");
        pending.setImgPath("/pending.jpg");
        pending.setCityId("city-1");
        pending.setKey("pending-parish");
        pending.setState(Parish.State.PENDING);
        parishes.save(pending);
        for (Parish.State state : List.of(Parish.State.APPROVED, Parish.State.DISABLED)) {
            Parish excluded = new Parish();
            excluded.setId(state.name().toLowerCase());
            excluded.setName(state.name());
            excluded.setCityId("city-1");
            excluded.setKey(state.name().toLowerCase());
            excluded.setState(state);
            parishes.save(excluded);
        }
        when(meilisearchReader.searchNearestMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());
        MvcResult result = mvc.perform(get(URL + "/map")
                        .param("dateTimeFrom", "2024-06-24T00:00:00+03:00"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
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
        Parish pending = new Parish();
        pending.setName("Pending");
        pending.setCityId("city-1");
        pending.setState(Parish.State.PENDING);
        pending = parishes.save(pending);
        when(meilisearchReader.searchNearestMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());
        MvcResult result = mvc.perform(get(URL + "/map").param("dateTimeFrom", "2024-06-24T00:00:00+03:00")
                        .param("dateTimeTo", "2024-06-25T12:00:00+03:00"))
                .andExpect(status().isOk()).andReturn();
        ArgumentCaptor<MassSearchFilter> filter = ArgumentCaptor.forClass(MassSearchFilter.class);
        verify(meilisearchReader).searchNearestMasses(filter.capture());
        assertThat(filter.getValue().dateTimeTo()).isEqualTo(OffsetDateTime.parse("2024-06-25T12:00:00+03:00"));
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", pending.getId())));
    }

    @Test void mapUnapprovedParishesCacheRefreshesAfterStateChange() throws Exception {
        Parish pending = new Parish();
        pending.setName("Pending");
        pending.setCityId("city-1");
        pending.setState(Parish.State.PENDING);
        pending = parishes.save(pending);
        when(meilisearchReader.searchNearestMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());
        String path = URL + "/map";
        String date = "2024-06-24T00:00:00+03:00";
        JsonNode initial = json.readTree(mvc.perform(get(path).param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(initial).isEqualTo(json.readTree(fixture("initial-response.json").replace("${id}", pending.getId())));
        Cache cache = caches.getCache("unapprovedParishes");
        assertThat(cache).isNotNull();
        Object cachedList = cache.get("be").get();
        assertThat(cachedList).isInstanceOf(List.class);
        JsonNode repeated = json.readTree(mvc.perform(get(path).param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(repeated).isEqualTo(initial);
        assertThat(cache.get("be").get()).isSameAs(cachedList);
        mvc.perform(put("/api/parish/{id}/state", pending.getId()).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("state-request.json")))
                .andExpect(status().isOk());
        assertThat(parishes.findById(pending.getId()).orElseThrow().getState())
                .isEqualTo(Parish.State.APPROVED);
        assertThat(cache.get("be")).isNull();
        JsonNode after = json.readTree(mvc.perform(get(path).param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(after).isEqualTo(json.readTree(fixture("after-response.json")));
        assertThat(cache.get("be").get()).isInstanceOf(List.class);
    }

    @Test void mapUnapprovedParishesCacheRefreshesAfterSaveAndDelete() throws Exception {
        when(meilisearchReader.searchNearestMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());
        String date = "2024-06-24T00:00:00+03:00";
        MvcResult empty = mvc.perform(get(URL + "/map").param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(empty.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("empty-response.json")));
        assertThat(caches.getCache("unapprovedParishes").get("be")).isNotNull();

        Parish pending = new Parish();
        pending.setId("saved-pending");
        pending.setName("Saved pending");
        pending.setCityId("city-1");
        pending.setState(Parish.State.PENDING);
        parishes.save(pending);
        assertThat(caches.getCache("unapprovedParishes").get("be")).isNull();
        MvcResult added = mvc.perform(get(URL + "/map").param("dateTimeFrom", date).param("lang", "be"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(added.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("added-response.json")));

        parishes.deleteParishById(pending.getId());
        assertThat(caches.getCache("unapprovedParishes").get("be")).isNull();
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
        City city = cities.save(City.builder().id("city-1").name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setId("parish-1");
        parish.setName("St. Mary");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parishes.save(parish);

        MvcResult result = mvc.perform(get(URL + "/week")
                        .param("parishId", parish.getId()).param("date", "24-06-2024")
                        .param("online", "true").param("rorate", "true").param("massLang", "pl"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
    }

    @Test void indexedWeekKeepsNavigationForParishWithoutMasses() throws Exception {
        City city = cities.save(City.builder().id("city-1").name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setId("parish-1");
        parish.setName("St. Mary");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parishes.save(parish);
        when(meilisearchReader.searchAllMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());

        MvcResult result = mvc.perform(get(URL + "/week-indexed")
                        .cookie(new Cookie("cityId", city.getId()))
                        .param("parishId", parish.getId()).param("date", "24-06-2024")
                        .param("online", "true").param("rorate", "true").param("massLang", "pl"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
        ArgumentCaptor<MassSearchFilter> filter = ArgumentCaptor.forClass(MassSearchFilter.class);
        verify(meilisearchReader).searchAllMasses(filter.capture());
        assertThat(filter.getValue().cityId()).isEqualTo(city.getId());
        assertThat(filter.getValue().parishId()).isEqualTo(parish.getId());
    }

    @Test void weekDefaultsToTodayAndDefaultCityWithoutCookie() throws Exception {
        City city = cities.save(City.builder().id("city-1").name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setKey("st-mary");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass mass = saved(parish.getId());
        mass.setDays(new int[]{7});
        mass = masses.save(mass);
        MvcResult result = mvc.perform(get(URL + "/week"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${parishId}", parish.getId())
                .replace("${massId}", mass.getId())
                .replace("${lastModifiedDate}", checkedModified(body.path("schedule").get(0)
                        .path("massHours").get(0).path("data").get(0)))));
    }

    @Test void cachedCityWeekAfterBulkDeleteHasNoMasses() throws Exception {
        City city = cities.save(City.builder().id("city-1").name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setKey("st-mary");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass mass = saved(parish.getId());
        mass.setDays(new int[]{7});
        mass = masses.save(mass);
        MvcResult first = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        JsonNode expected = json.readTree(fixture("response.json")
                .replace("${parishId}", parish.getId())
                .replace("${massId}", mass.getId())
                .replace("${lastModifiedDate}", checkedModified(json.readTree(first.getResponse()
                        .getContentAsString(StandardCharsets.UTF_8)).path("schedule").get(0)
                        .path("massHours").get(0).path("data").get(0))));
        assertThat(json.readTree(first.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(expected);
        MvcResult deletion = mvc.perform(delete(URL).param("parishId", parish.getId())
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(deletion.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("delete-response.json").replace("${massId}", mass.getId())));
        assertThat(masses.findById(mass.getId())).isEmpty();
        MvcResult after = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(after.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("schedule").isEmpty()).isTrue();
    }

    @Test void cachedCityWeekExcludesParishAfterItBecomesPending() throws Exception {
        City city = cities.save(City.builder().id("city-1").name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setKey("st-mary");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass mass = saved(parish.getId());
        mass.setDays(new int[]{7});
        mass = masses.save(mass);
        MvcResult first = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        JsonNode expected = json.readTree(fixture("response.json")
                .replace("${parishId}", parish.getId())
                .replace("${massId}", mass.getId())
                .replace("${lastModifiedDate}", checkedModified(json.readTree(first.getResponse()
                        .getContentAsString(StandardCharsets.UTF_8)).path("schedule").get(0)
                        .path("massHours").get(0).path("data").get(0))));
        assertThat(json.readTree(first.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(expected);
        MvcResult state = mvc.perform(put("/api/parish/{id}/state", parish.getId())
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("state-request.json")))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(state.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("state-response.json").replace("${parishId}", parish.getId())));
        assertThat(parishes.findById(parish.getId()).orElseThrow().getState())
                .isEqualTo(Parish.State.PENDING);
        assertThat(masses.findById(mass.getId())).isPresent();
        MvcResult after = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(after.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("after-response.json")));
    }

    @Test void cachedCityWeekIncludesParishAfterItBecomesApproved() throws Exception {
        City city = cities.save(City.builder().id("city-1").name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setKey("st-mary");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.PENDING);
        parish = parishes.save(parish);
        Mass mass = saved(parish.getId());
        mass.setDays(new int[]{7});
        mass = masses.save(mass);
        MvcResult first = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(first.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("before-response.json")));
        MvcResult state = mvc.perform(put("/api/parish/{id}/state", parish.getId())
                        .header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("state-request.json")))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(state.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("state-response.json").replace("${parishId}", parish.getId())));
        assertThat(parishes.findById(parish.getId()).orElseThrow().getState())
                .isEqualTo(Parish.State.APPROVED);
        assertThat(masses.findById(mass.getId())).isPresent();
        MvcResult after = mvc.perform(get(URL + "/week")).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(after.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${parishId}", parish.getId())
                .replace("${massId}", mass.getId())
                .replace("${lastModifiedDate}", checkedModified(body.path("schedule").get(0)
                        .path("massHours").get(0).path("data").get(0)))));
    }

    @Test void indexedWeekDefaultsToTodayAndDefaultCityWithoutCookie() throws Exception {
        City city = cities.save(City.builder().name("Minsk").key("minsk").build());
        when(meilisearchReader.searchAllMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of()).facetDistribution(Map.of()).build());
        JsonNode body = json.readTree(mvc.perform(get(URL + "/week-indexed"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.path("startWeekDate").asText()).isEqualTo("07/16/2023");
        assertThat(body.path("schedule").isEmpty()).isTrue();
        ArgumentCaptor<MassSearchFilter> filter = ArgumentCaptor.forClass(MassSearchFilter.class);
        verify(meilisearchReader).searchAllMasses(filter.capture());
        assertThat(filter.getValue().cityId()).isEqualTo(city.getId());
        assertThat(filter.getValue().dateTimeFrom()).isEqualTo(OffsetDateTime.parse("2023-07-16T00:00:00+03:00"));
        assertThat(filter.getValue().dateTimeTo().toLocalDate()).isEqualTo(LocalDate.of(2023, 7, 22));
    }

    @Test void weekWithoutParishUsesCityAndExcludesPendingParishes() throws Exception {
        City selected = cities.save(City.builder().name("Selected").key("selected").build());
        City otherCity = cities.save(City.builder().name("Other").key("other").build());
        Parish approved = new Parish();
        approved.setName("Approved");
        approved.setKey("approved");
        approved.setCityId(selected.getId());
        approved.setState(Parish.State.APPROVED);
        approved = parishes.save(approved);
        Parish pending = new Parish();
        pending.setName("Pending");
        pending.setKey("pending");
        pending.setCityId(selected.getId());
        pending.setState(Parish.State.PENDING);
        pending = parishes.save(pending);
        Parish elsewhere = new Parish();
        elsewhere.setName("Elsewhere");
        elsewhere.setKey("elsewhere");
        elsewhere.setCityId(otherCity.getId());
        elsewhere.setState(Parish.State.APPROVED);
        elsewhere = parishes.save(elsewhere);
        Mass visible = saved(approved.getId());
        visible.setCityId(selected.getId());
        visible = masses.save(visible);
        Mass hidden = saved(pending.getId());
        hidden.setCityId(selected.getId());
        masses.save(hidden);
        Mass other = saved(elsewhere.getId());
        other.setCityId(otherCity.getId());
        masses.save(other);
        JsonNode result = json.readTree(mvc.perform(get(URL + "/week")
                        .cookie(new Cookie("cityId", selected.getId())).param("date", "24-06-2024"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(result.path("schedule").size()).isEqualTo(1);
        assertThat(result.path("schedule").get(0).path("massHours").get(0).path("data").get(0).path("id").asText())
                .isEqualTo(visible.getId());
    }

    @Test void weekReturnsApprovedParishMass() throws Exception {
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setCityId("city-1");
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass mass = saved(parish.getId());
        MvcResult result = mvc.perform(get(URL + "/week")
                        .param("parishId", parish.getId()).param("date", "24-06-2024"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${parishId}", parish.getId())
                .replace("${massId}", mass.getId())
                .replace("${lastModifiedDate}", checkedModified(body.path("schedule").get(0).path("massHours").get(0).path("data").get(0)))));
    }

    @Test void weekFiltersOnlineRorateAndLanguage() throws Exception {
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setCityId("city-1");
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass matching = saved(parish.getId());
        matching.setOnline(true);
        matching.setRorate(true);
        matching = masses.save(matching);
        Mass offline = new Mass(matching);
        offline.setTime("09:00");
        offline.setOnline(false);
        offline.setRorate(true);
        masses.save(offline);
        Mass otherLanguage = new Mass(matching);
        otherLanguage.setTime("10:00");
        otherLanguage.setOnline(true);
        otherLanguage.setRorate(true);
        otherLanguage.setLangCode("pl");
        masses.save(otherLanguage);
        Mass notRorate = new Mass(matching);
        notRorate.setTime("11:00");
        notRorate.setOnline(true);
        notRorate.setRorate(false);
        masses.save(notRorate);
        JsonNode body = json.readTree(mvc.perform(get(URL + "/week")
                        .param("parishId", parish.getId()).param("date", "24-06-2024")
                        .param("online", "true").param("rorate", "true").param("massLang", "be"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.path("schedule").size()).isEqualTo(1);
        JsonNode data = body.path("schedule").get(0).path("massHours").get(0).path("data");
        assertThat(data.size()).isEqualTo(1);
        assertThat(data.get(0).path("id").asText()).isEqualTo(matching.getId());
        assertThat(body.path("nav").path("selected").path("online").path("value").asText()).isEqualTo("true");
        assertThat(body.path("nav").path("selected").path("rorate").path("value").asText()).isEqualTo("true");
        assertThat(body.path("nav").path("selected").path("lang").path("value").asText()).isEqualTo("be");
    }

    @Test void weekIncludesPendingParishOnlyWhenRequested() throws Exception {
        Parish parish = new Parish();
        parish.setName("Pending parish");
        parish.setCityId("city-1");
        parish.setState(Parish.State.PENDING);
        parish = parishes.save(parish);
        Mass mass = saved(parish.getId());
        JsonNode hidden = json.readTree(mvc.perform(get(URL + "/week")
                        .param("parishId", parish.getId()).param("date", "24-06-2024"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(hidden.path("schedule").isEmpty()).isTrue();
        JsonNode shown = json.readTree(mvc.perform(get(URL + "/week")
                        .param("parishId", parish.getId()).param("date", "24-06-2024")
                        .header("x-show-pending", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(shown.path("schedule").size()).isEqualTo(1);
        assertThat(shown.path("schedule").get(0).path("massHours").get(0).path("data").get(0).path("id").asText())
                .isEqualTo(mass.getId());
    }

    @Test void indexedWeekReturnsIndexedMass() throws Exception {
        City city = cities.save(City.builder().name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setName("St. Mary");
        parish.setKey("st-mary");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        when(meilisearchReader.searchAllMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of(indexedMass(city.getId(), parish.getId())))
                        .facetDistribution(Map.of()).build());
        MvcResult result = mvc.perform(get(URL + "/week-indexed")
                        .param("parishId", parish.getId())
                        .cookie(new Cookie("cityId", city.getId()))
                        .param("date", "24-06-2024"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${cityId}", city.getId()).replace("${parishId}", parish.getId())));
        ArgumentCaptor<MassSearchFilter> filter = ArgumentCaptor.forClass(MassSearchFilter.class);
        verify(meilisearchReader).searchAllMasses(filter.capture());
        assertThat(filter.getValue().cityId()).isEqualTo(city.getId());
        assertThat(filter.getValue().parishId()).isEqualTo(parish.getId());
        assertThat(filter.getValue().dateTimeFrom().toLocalDate()).isEqualTo(LocalDate.of(2024, 6, 24));
        assertThat(filter.getValue().dateTimeTo().toLocalDate()).isEqualTo(LocalDate.of(2024, 6, 30));
        assertThat(filter.getValue().online()).isNull();
        assertThat(filter.getValue().rorate()).isNull();
        assertThat(filter.getValue().lang()).isNull();
    }

    @Test void indexedWeekPassesFilters() throws Exception {
        City city = cities.save(City.builder().name("Minsk").key("minsk").build());
        Parish parish = new Parish();
        parish.setName("Approved");
        parish.setCityId(city.getId());
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        SearchResultItem hit = new SearchResultItem("record-1", "mass-1", 60,
                LocalDateTime.of(2024, 6, 24, 8, 0),
                new by.imsha.meilisearch.model.Parish(parish.getId(), true, "APPROVED"),
                "Morning mass", "be", true, true,
                new by.imsha.meilisearch.model.City(city.getId()), null,
                new Geo(53.9, 27.57), 120L);
        when(meilisearchReader.searchAllMasses(any(MassSearchFilter.class)))
                .thenReturn(SearchResult.builder().hits(List.of(hit)).facetDistribution(Map.of()).build());
        JsonNode result = json.readTree(mvc.perform(get(URL + "/week-indexed")
                        .cookie(new Cookie("cityId", city.getId())).param("date", "24-06-2024")
                        .param("online", "true").param("rorate", "true").param("massLang", "be")
                        .header("x-show-pending", "false"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        ArgumentCaptor<MassSearchFilter> filter = ArgumentCaptor.forClass(MassSearchFilter.class);
        verify(meilisearchReader).searchAllMasses(filter.capture());
        assertThat(filter.getValue().online()).isTrue();
        assertThat(filter.getValue().rorate()).isTrue();
        assertThat(filter.getValue().lang()).isEqualTo("be");
        assertThat(result.path("schedule").size()).isEqualTo(1);
    }

    @Test void parishWeekQueryKeyReturnsSchedule() throws Exception {
        Parish parish = new Parish();
        parish.setName("Mapped parish");
        parish.setKey("123");
        parish.setCityId("city-1");
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass mass = saved(parish.getId());
        MvcResult result = mvc.perform(get(URL + "/parish-week").param("apiKey", "123"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode data = body.get(0).path("massHours").get(0).path("data").get(0);
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${parishId}", parish.getId())
                .replace("${massId}", mass.getId())
                .replace("${lastModifiedDate}", checkedModified(data))));
    }

    @Test void parishWeekHeaderKeyReturnsSchedule() throws Exception {
        Parish parish = new Parish();
        parish.setName("Mapped parish");
        parish.setKey("123");
        parish.setCityId("city-1");
        parish.setState(Parish.State.APPROVED);
        parish = parishes.save(parish);
        Mass mass = saved(parish.getId());
        MvcResult result = mvc.perform(get(URL + "/parish-week").header("parish-week-api-key", "123"))
                .andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode data = body.get(0).path("massHours").get(0).path("data").get(0);
        assertThat(body).isEqualTo(json.readTree(fixture("response.json")
                .replace("${parishId}", parish.getId())
                .replace("${massId}", mass.getId())
                .replace("${lastModifiedDate}", checkedModified(data))));
    }
}
