package by.imsha.rest;

import by.imsha.Application;
import by.imsha.domain.City;
import by.imsha.domain.LocalizedCity;
import by.imsha.repository.CityRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = Application.class)
@Import(FixedClockTestConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "qa"})
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CityControllerMockMvcIT {
    private static final String URL = "/api/cities";
    private static final String API_KEY = "TEST_API_KEY";

    @Container
    static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);

    // Feign clients are pointed at a controlled endpoint even though city requests do not call them.
    static final WireMockServer IDENTITY = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        IDENTITY.start();
    }

    @AfterAll
    static void stopIdentityStub() {
        IDENTITY.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_city_mockmvc");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
    }

    @Autowired MockMvc mvc;
    @Autowired CityRepository cities;
    @Autowired CacheManager cacheManager;
    @Autowired ObjectMapper objectMapper;
    private String caseDirectory;

    @BeforeEach
    void isolate(TestInfo testInfo) {
        caseDirectory = "city-mockmvc/" + testInfo.getTestMethod().orElseThrow().getName() + "/";
        cities.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        IDENTITY.resetAll();
    }

    private String fixture(String name) throws Exception {
        return new ClassPathResource(caseDirectory + name).getContentAsString(StandardCharsets.UTF_8);
    }

    private City city(String key, String name) {
        return cities.save(City.builder().key(key).name(name).build());
    }

    private void assertJson(MvcResult result, String resource, String id) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        String expected = fixture(resource).replace("${id}", id == null ? "" : id);
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(objectMapper.readTree(expected));
    }

    private void assertJson(MvcResult result, String resource) throws Exception {
        assertJson(result, resource, null);
    }

    private ResultMatcher createdCity(String resource) {
        return result -> {
            JsonNode id = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("id");
            assertThat(id.isTextual()).isTrue();
            assertThat(id.asText()).matches("[0-9a-f]{24}");
            assertJson(result, resource, id.asText());
        };
    }

    private static void assertUnauthorized(MvcResult result) throws Exception {
        assertThat(result.getResponse().getContentAsString()).isEmpty();
        assertThat(result.getResponse().getContentType()).isNull();
        assertThat(result.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }

    private List<JsonNode> pageContent(MvcResult result, int expectedSize) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        JsonNode page = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        List<String> fields = new ArrayList<>();
        fields.addAll(page.propertyNames());
        assertThat(fields).containsExactlyInAnyOrder(
                "content", "pageable", "last", "totalPages", "totalElements", "size", "number",
                "sort", "first", "numberOfElements", "empty");
        assertThat(page.path("pageable").path("pageNumber").asInt()).isEqualTo(page.path("number").asInt());
        assertThat(page.path("pageable").path("pageSize").asInt()).isEqualTo(page.path("size").asInt());
        assertThat(page.path("pageable").path("offset").asLong())
                .isEqualTo((long) page.path("number").asInt() * page.path("size").asInt());
        assertThat(page.path("empty").asBoolean()).isFalse();
        JsonNode content = page.path("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content.size()).isEqualTo(expectedSize);
        List<JsonNode> elements = new ArrayList<>();
        content.forEach(elements::add);
        return elements;
    }

    @Nested class Create {
        @Test void validRequestPersistsCity() throws Exception {
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("create-valid.json")))
                    .andExpect(status().isCreated())
                    .andExpect(createdCity("create-response.json"));
            assertThat(cities.findByKey("city-pilot")).isNotNull();
        }

        @Test void keyIsTrimmedBeforePersistenceAndSerialization() throws Exception {
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("request.json")))
                    .andExpect(status().isCreated())
                    .andExpect(createdCity("response.json"));
            assertThat(cities.findByKey("city-trimmed")).extracting(City::getName)
                    .isEqualTo("Trimmed key city");
        }

        @Test void missingNameReturnsValidationError() throws Exception {
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("create-no-name.json")))
                    .andExpect(status().isBadRequest())
                    .andDo(result -> assertJson(result, "validation-error.json"));
        }

        @Test void emptyNameReturnsValidationError() throws Exception {
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("request.json")))
                    .andExpect(status().isBadRequest())
                    .andDo(result -> assertJson(result, "error.json"));
            assertThat(cities.count()).isZero();
        }

        @Test void createWhitespaceNamePassesNotEmptyValidation() throws Exception {
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("request.json")))
                    .andExpect(status().isCreated())
                    .andExpect(createdCity("response.json"));
            assertThat(cities.findByKey("city-spaces")).extracting(City::getName).isEqualTo("   ");
        }

        @Test void emptyObjectReturnsValidationError() throws Exception {
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("empty-object.json")))
                    .andExpect(status().isBadRequest())
                    .andDo(result -> assertJson(result, "validation-error.json"));
        }

        @Test void unknownFieldsOnlyReturnValidationError() throws Exception {
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("create-unknown-fields.json")))
                    .andExpect(status().isBadRequest())
                    .andDo(result -> assertJson(result, "validation-error.json"));
        }

        @Test void secondCityWithoutKeyFails() throws Exception {
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("create-no-key-first.json")))
                    .andExpect(status().isCreated())
                    .andExpect(createdCity("create-no-key-first-response.json"));
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("create-no-key-second.json")))
                    .andExpect(status().isInternalServerError())
                    .andDo(result -> assertJson(result, "create-duplicate-error.json"));
            assertThat(cities.count()).isEqualTo(1);
            assertThat(cities.findAll()).singleElement().extracting(City::getName)
                    .isEqualTo("First city without key");
        }

        @Test void duplicateNameFails() throws Exception {
            city("existing-key", "Pilot city");
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("create-valid.json")))
                    .andExpect(status().isInternalServerError())
                    .andDo(result -> assertJson(result, "create-duplicate-error.json"));
            assertThat(cities.count()).isEqualTo(1);
            assertThat(cities.findByKey("existing-key")).extracting(City::getName).isEqualTo("Pilot city");
        }

        @Test void duplicateKeyFailsAndDoesNotAddCity() throws Exception {
            city("city-pilot", "Existing city");
            mvc.perform(post(URL).header("X-Api-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("create-valid.json")))
                    .andExpect(status().isInternalServerError())
                    .andDo(result -> assertJson(result, "create-duplicate-error.json"));
            assertThat(cities.count()).isEqualTo(1);
            assertThat(cities.findByKey("city-pilot")).extracting(City::getName).isEqualTo("Existing city");
        }

        @Test void anonymousCannotCreate() throws Exception {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(fixture("create-valid.json")))
                    .andExpect(status().isUnauthorized()).andDo(CityControllerMockMvcIT::assertUnauthorized);
            assertThat(cities.count()).isZero();
        }

        @Test void invalidApiKeyCannotCreate() throws Exception {
            mvc.perform(post(URL).header("X-Api-Key", "wrong-key").contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("create-valid.json")))
                    .andExpect(status().isUnauthorized()).andDo(CityControllerMockMvcIT::assertUnauthorized);
            assertThat(cities.count()).isZero();
        }

        @Test void internalApiKeyCanCreate() throws Exception {
            mvc.perform(post(URL).header("X-Api-Key", "TEST_INTERNAL_API_KEY")
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("create-valid.json")))
                    .andExpect(status().isCreated())
                    .andExpect(createdCity("create-response.json"));
        }
    }

    @Nested class ReadOne {
        @Test void existingCityIsReturned() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "get-response.json", city.getId()));
        }

        @Test void readOneUnknownCityReturns404() throws Exception {
            mvc.perform(get(URL + "/missing").header("X-Api-Key", API_KEY))
                    .andExpect(status().isNotFound())
                    .andDo(result -> assertJson(result, "get-missing-error.json"));
        }

        @Test void anonymousCannotRead() throws Exception {
            mvc.perform(get(URL + "/missing")).andExpect(status().isUnauthorized()).andDo(CityControllerMockMvcIT::assertUnauthorized);
        }
    }

    @Nested class ReadList {
        @Test void pageBeyondLastIsEmptyButKeepsTotals() throws Exception {
            city("city-first", "First city");
            city("city-second", "Second city");
            city("city-third", "Third city");
            mvc.perform(get(URL + "?page=5&size=2").header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json"));
        }

        @Test void emptyListUsesDefaultPagination() throws Exception {
            mvc.perform(get(URL).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json"));
        }

        @Test void pageAndSizeAreApplied() throws Exception {
            City first = city("city-first", "First city");
            City second = city("city-second", "Second city");
            City third = city("city-third", "Third city");
            MvcResult firstPage = mvc.perform(get(URL + "?page=0&size=2").header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(false))
                    .andExpect(jsonPath("$.numberOfElements").value(2))
                    .andReturn();
            MvcResult secondPage = mvc.perform(get(URL + "?page=1&size=2").header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.size").value(2))
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.first").value(false))
                    .andExpect(jsonPath("$.last").value(true))
                    .andExpect(jsonPath("$.numberOfElements").value(1))
                    .andReturn();
            MvcResult completePage = mvc.perform(get(URL + "?page=0&size=3").header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk()).andReturn();
            assertThat(pageContent(completePage, 3)).containsExactlyInAnyOrder(
                    objectMapper.valueToTree(first),
                    objectMapper.valueToTree(second),
                    objectMapper.valueToTree(third));
            for (JsonNode item : pageContent(firstPage, 2)) {
                assertThat(item).isIn(objectMapper.valueToTree(first), objectMapper.valueToTree(second), objectMapper.valueToTree(third));
            }
            for (JsonNode item : pageContent(secondPage, 1)) {
                assertThat(item).isIn(objectMapper.valueToTree(first), objectMapper.valueToTree(second), objectMapper.valueToTree(third));
            }
        }

        @Test void anonymousCannotList() throws Exception {
            mvc.perform(get(URL)).andExpect(status().isUnauthorized()).andDo(CityControllerMockMvcIT::assertUnauthorized);
        }

        @Test void negativePageIsRejected() throws Exception {
            mvc.perform(get(URL + "?page=-1").header("X-Api-Key", API_KEY))
                    .andExpect(status().isBadRequest());
        }

        @Test void zeroSizeIsRejected() throws Exception {
            mvc.perform(get(URL + "?size=0").header("X-Api-Key", API_KEY))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested class Update {
        @Test void emptyObjectKeepsExistingCity() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(put(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json", city.getId()));
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "get-response.json", city.getId()));
            assertThat(cities.findById(city.getId())).get().satisfies(saved -> {
                assertThat(saved.getName()).isEqualTo("Pilot city");
                assertThat(saved.getKey()).isEqualTo("city-pilot");
            });
        }

        @Test void duplicateNameDoesNotPersistPartialUpdate() throws Exception {
            City source = city("source-key", "Source city");
            City occupied = city("occupied-key", "Occupied city");
            mvc.perform(put(URL + "/{id}", source.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                    .andExpect(status().isInternalServerError())
                    .andDo(result -> assertJson(result, "error.json", source.getId()));
            assertThat(cities.count()).isEqualTo(2);
            assertThat(cities.findById(source.getId())).get().satisfies(saved -> {
                assertThat(saved.getName()).isEqualTo("Source city");
                assertThat(saved.getKey()).isEqualTo("source-key");
            });
            assertThat(cities.findById(occupied.getId())).get().satisfies(saved -> {
                assertThat(saved.getName()).isEqualTo("Occupied city");
                assertThat(saved.getKey()).isEqualTo("occupied-key");
            });
        }

        @Test void duplicateKeyDoesNotPersistPartialUpdate() throws Exception {
            City source = city("source-key", "Source city");
            City occupied = city("occupied-key", "Occupied city");
            mvc.perform(put(URL + "/{id}", source.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                    .andExpect(status().isInternalServerError())
                    .andDo(result -> assertJson(result, "error.json", source.getId()));
            assertThat(cities.count()).isEqualTo(2);
            assertThat(cities.findById(source.getId())).get().satisfies(saved -> {
                assertThat(saved.getName()).isEqualTo("Source city");
                assertThat(saved.getKey()).isEqualTo("source-key");
            });
            assertThat(cities.findById(occupied.getId())).get().satisfies(saved -> {
                assertThat(saved.getName()).isEqualTo("Occupied city");
                assertThat(saved.getKey()).isEqualTo("occupied-key");
            });
        }

        @Test void omittedFieldsKeepTheirPreviousValues() throws Exception {
            City city = city("old-key", "Old city");
            mvc.perform(put(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("name-only-request.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json", city.getId()));
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "after-name-response.json", city.getId()));

            mvc.perform(put(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("key-only-request.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json", city.getId()));
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "after-key-response.json", city.getId()));
            assertThat(cities.findById(city.getId())).get().satisfies(saved -> {
                assertThat(saved.getName()).isEqualTo("New city");
                assertThat(saved.getKey()).isEqualTo("new-key");
            });
        }

        @Test void validRequestUpdatesCity() throws Exception {
            City city = city("city-pilot", "Old city");
            mvc.perform(put(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("update-valid.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "updated-response.json", city.getId()));
            assertThat(cities.findById(city.getId())).get().extracting(City::getName).isEqualTo("Updated city");
        }

        @Test void keyIsTrimmedBeforeUpdate() throws Exception {
            City city = city("old-key", "Old city");
            mvc.perform(put(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("request.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json", city.getId()));
            assertThat(cities.findById(city.getId())).get().extracting(City::getKey).isEqualTo("updated-key");
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "get-response.json", city.getId()));
        }

        @Test void missingBodyReturns400() throws Exception {
            mvc.perform(put(URL + "/missing").header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andDo(result -> assertJson(result, "update-missing-body-error.json"));
        }

        @Test void updateUnknownCityReturns404() throws Exception {
            mvc.perform(put(URL + "/missing").header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("update-valid.json")))
                    .andExpect(status().isNotFound())
                    .andDo(result -> assertJson(result, "update-missing-error.json"));
        }

        @Test void emptyObjectAndUnknownCityReturns404() throws Exception {
            mvc.perform(put(URL + "/missing").header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("empty-object.json")))
                    .andExpect(status().isNotFound())
                    .andDo(result -> assertJson(result, "update-missing-error.json"));
        }

        @Test void anonymousCannotUpdate() throws Exception {
            mvc.perform(put(URL + "/missing").contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("update-valid.json")))
                    .andExpect(status().isUnauthorized()).andDo(CityControllerMockMvcIT::assertUnauthorized);
        }
    }

    @Nested class Localize {
        @Test void localizeUnknownCityReturns404() throws Exception {
            mvc.perform(put(URL + "/missing/lang/be").header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                    .andExpect(status().isNotFound())
                    .andDo(result -> assertJson(result, "error.json"));
        }

        @Test void repeatedLocalizationReplacesOnlyThatLanguage() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(put(URL + "/{id}/lang/pl", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("polish-request.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json", city.getId()));
            mvc.perform(put(URL + "/{id}/lang/be", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("first-belarusian-request.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json", city.getId()));
            mvc.perform(put(URL + "/{id}/lang/be", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("second-belarusian-request.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json", city.getId()));
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "get-response.json", city.getId()));
        }

        @Test void validLocaleUpdatesCity() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(put(URL + "/{id}/lang/be", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("localize-valid.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "updated-response.json", city.getId()));
            assertThat(cities.findById(city.getId())).get().satisfies(saved ->
                    assertThat(((LocalizedCity) saved.getLocalizedInfo().get("be")).getName()).isEqualTo("Пілотны горад"));
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "get-response.json", city.getId()));
        }

        @Test void regionLocaleIsStoredUnderLanguageOnly() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(put(URL + "/{id}/lang/be-BY", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("request.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json", city.getId()));
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "get-response.json", city.getId()));
        }

        @Test void missingNameReturns400() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(put(URL + "/{id}/lang/be", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("localize-no-name.json")))
                    .andExpect(status().isBadRequest())
                    .andDo(result -> assertJson(result, "localize-missing-name-error.json", city.getId()));
        }

        @Test void emptyNameReturns400() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(put(URL + "/{id}/lang/be", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("request.json")))
                    .andExpect(status().isBadRequest())
                    .andDo(result -> assertJson(result, "error.json", city.getId()));
            assertThat(cities.findById(city.getId())).get().satisfies(saved ->
                    assertThat(saved.getLocalizedInfo()).isEmpty());
        }

        @Test void localizeWhitespaceNamePassesNotEmptyValidation() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(put(URL + "/{id}/lang/be", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("request.json")))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "response.json", city.getId()));
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "get-response.json", city.getId()));
        }

        @Test void unavailableLocaleReturns400() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(put(URL + "/{id}/lang/zz-ZZ", city.getId()).header("X-Api-Key", API_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("localize-valid.json")))
                    .andExpect(status().isBadRequest())
                    .andDo(result -> assertJson(result, "localize-invalid-locale-error.json", city.getId()));
        }

        @Test void anonymousCannotLocalize() throws Exception {
            mvc.perform(put(URL + "/missing/lang/be").contentType(MediaType.APPLICATION_JSON)
                            .content(fixture("localize-valid.json")))
                    .andExpect(status().isUnauthorized()).andDo(CityControllerMockMvcIT::assertUnauthorized);
        }
    }

    @Nested class Delete {
        @Test void cachedCityReadAfterDeleteReturnsNotFound() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "get-response.json", city.getId()));
            mvc.perform(delete(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "deleted-response.json", city.getId()));
            assertThat(cities.findById(city.getId())).isEmpty();
            mvc.perform(get(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isNotFound());
        }
        @Test void existingCityIsDeleted() throws Exception {
            City city = city("city-pilot", "Pilot city");
            mvc.perform(delete(URL + "/{id}", city.getId()).header("X-Api-Key", API_KEY))
                    .andExpect(status().isOk())
                    .andDo(result -> assertJson(result, "deleted-response.json", city.getId()));
            assertThat(cities.findById(city.getId())).isEmpty();
        }

        @Test void deleteUnknownCityReturns404() throws Exception {
            mvc.perform(delete(URL + "/missing").header("X-Api-Key", API_KEY))
                    .andExpect(status().isNotFound())
                    .andDo(result -> assertJson(result, "delete-missing-error.json"));
        }

        @Test void anonymousCannotDelete() throws Exception {
            mvc.perform(delete(URL + "/missing")).andExpect(status().isUnauthorized()).andDo(CityControllerMockMvcIT::assertUnauthorized);
        }
    }
}
