package by.imsha.rest;

import by.imsha.Application;
import by.imsha.domain.Cors;
import by.imsha.repository.CorsConfigRepository;
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
import java.util.List;

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
class CorsControllerMockMvcIT {
    private static final String URL = "/api/cors";
    private static final String KEY = "TEST_API_KEY";
    @Container static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
    static final WireMockServer IDENTITY = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    static { IDENTITY.start(); }
    @AfterAll static void stopStub() { IDENTITY.stop(); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_cors_mockmvc");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
    }
    @Autowired MockMvc mvc;
    @Autowired CorsConfigRepository cors;
    @Autowired CacheManager caches;
    @Autowired ObjectMapper json;
    private String directory;

    @BeforeEach void isolate(TestInfo info) {
        directory = "cors-mockmvc/" + info.getTestMethod().orElseThrow().getName() + "/";
        cors.deleteAll();
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        IDENTITY.resetAll();
    }
    private String fixture(String name) throws Exception {
        return new ClassPathResource(directory + name).getContentAsString(StandardCharsets.UTF_8);
    }
    private Cors saved(String origin) { return cors.save(Cors.builder().origin(origin).build()); }
    private void assertJson(MvcResult result, String id) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", id)));
    }
    @Test void createPersistsOrigin() throws Exception {
        MvcResult result = mvc.perform(post(URL).header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isCreated()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        String id = body.path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        assertJson(result, id);
        assertThat(cors.findById(id).orElseThrow().getOrigin()).isEqualTo("https://example.org");
    }
    @Test void getExistingOrigin() throws Exception {
        Cors entry = saved("https://example.org");
        assertJson(mvc.perform(get(URL + "/" + entry.getId()).header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn(), entry.getId());
    }
    @Test void getMissingOrigin() throws Exception {
        assertJson(mvc.perform(get(URL + "/000000000000000000000000").header("X-Api-Key", KEY)).andExpect(status().isNotFound()).andReturn(), "");
    }
    @Test void listOrigins() throws Exception {
        Cors entry = saved("https://example.org");
        MvcResult result = mvc.perform(get(URL).param("page", "0").param("size", "1").header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        assertJson(result, entry.getId());
    }
    @Test void updateOrigin() throws Exception {
        Cors entry = saved("https://old.example.org");
        assertJson(mvc.perform(put(URL + "/" + entry.getId()).header("X-Api-Key", KEY).contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn(), entry.getId());
        assertThat(cors.findById(entry.getId()).orElseThrow().getOrigin()).isEqualTo("https://example.org");
    }
    @Test void deleteOrigin() throws Exception {
        Cors entry = saved("https://example.org");
        assertJson(mvc.perform(delete(URL + "/" + entry.getId()).header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn(), entry.getId());
        assertThat(cors.findById(entry.getId())).isEmpty();
    }
    @Test void anonymousCannotList() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized()).andDo(result -> {
            assertThat(result.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
            assertThat(result.getResponse().getContentAsString()).isEmpty();
        });
    }

    @Test void updateMissingOriginDoesNotCreateIt() throws Exception {
        String id = "000000000000000000000000";
        mvc.perform(put(URL + "/" + id).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isNotFound());
        assertThat(cors.count()).isZero();
    }

    @Test void deleteMissingOriginDoesNotChangeCollection() throws Exception {
        Cors existing = saved("https://example.org");
        mvc.perform(delete(URL + "/000000000000000000000000").header("X-Api-Key", KEY))
                .andExpect(status().isNotFound());
        assertThat(cors.findAll()).containsExactly(existing);
    }

    @Test void emptyOriginIsRejectedWithoutPersisting() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(cors.count()).isZero();
    }

    @Test void missingOriginIsRejectedWithoutPersisting() throws Exception {
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(cors.count()).isZero();
    }

    @Test void duplicateOriginIsRejectedWithoutPersistingAnotherEntry() throws Exception {
        Cors existing = saved("https://example.org");
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isInternalServerError());
        assertThat(cors.findAll()).containsExactly(existing);
    }

    @Test void missingOriginOnUpdateKeepsStoredValue() throws Exception {
        Cors existing = saved("https://example.org");
        mvc.perform(put(URL + "/" + existing.getId()).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk());
        assertThat(cors.findById(existing.getId()).orElseThrow().getOrigin()).isEqualTo("https://example.org");
    }

    @Test void emptyOriginOnUpdateIsRejectedWithoutChangingStoredValue() throws Exception {
        Cors existing = saved("https://example.org");
        mvc.perform(put(URL + "/" + existing.getId()).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(cors.findById(existing.getId()).orElseThrow().getOrigin()).isEqualTo("https://example.org");
    }

    @Test void listOriginsPaginatesWithoutDuplicates() throws Exception {
        Cors one = saved("https://one.example.org");
        Cors two = saved("https://two.example.org");
        MvcResult first = mvc.perform(get(URL).param("page", "0").param("size", "1")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        MvcResult second = mvc.perform(get(URL).param("page", "1").param("size", "1")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        MvcResult complete = mvc.perform(get(URL).param("page", "0").param("size", "2")
                        .header("X-Api-Key", KEY)).andExpect(status().isOk()).andReturn();
        JsonNode firstPage = json.readTree(first.getResponse().getContentAsString());
        JsonNode secondPage = json.readTree(second.getResponse().getContentAsString());
        assertThat(firstPage.path("number").asInt()).isZero();
        assertThat(secondPage.path("number").asInt()).isEqualTo(1);
        for (JsonNode page : new JsonNode[]{firstPage, secondPage}) {
            assertThat(page.path("size").asInt()).isEqualTo(1);
            assertThat(page.path("totalElements").asInt()).isEqualTo(2);
            assertThat(page.path("totalPages").asInt()).isEqualTo(2);
            assertThat(page.path("content").size()).isEqualTo(1);
        }
        JsonNode completePage = json.readTree(complete.getResponse().getContentAsString());
        assertThat(List.of(completePage.path("content").get(0).path("id").asText(),
                        completePage.path("content").get(1).path("id").asText()))
                .containsExactlyInAnyOrder(one.getId(), two.getId());
        assertThat(List.of(one.getId(), two.getId())).contains(firstPage.path("content").get(0).path("id").asText(),
                secondPage.path("content").get(0).path("id").asText());
    }

    @Test void storedOriginAllowsCredentialedPreflightAfterCreate() throws Exception {
        String origin = "https://dynamic.example.org";
        mvc.perform(options(URL).header("Origin", origin)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
        mvc.perform(post(URL).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isCreated());
        mvc.perform(options(URL).header("Origin", origin)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
                    assertThat(result.getResponse().getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
                });
    }

    @Test void updateAndDeleteInvalidateDynamicCorsCache() throws Exception {
        String oldOrigin = "https://old.example.org";
        String newOrigin = "https://new.example.org";
        Cors entry = saved(oldOrigin);
        assertPreflight(oldOrigin, 200);
        mvc.perform(put(URL + "/" + entry.getId()).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk());
        assertPreflight(oldOrigin, 403);
        assertPreflight(newOrigin, 200);
        mvc.perform(delete(URL + "/" + entry.getId()).header("X-Api-Key", KEY))
                .andExpect(status().isOk());
        assertPreflight(newOrigin, 403);
    }

    private void assertPreflight(String origin, int expectedStatus) throws Exception {
        MvcResult result = mvc.perform(options(URL).header("Origin", origin)
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        if (expectedStatus == 200) {
            assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
        }
    }
}
