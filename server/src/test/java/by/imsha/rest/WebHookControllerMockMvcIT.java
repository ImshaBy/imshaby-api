package by.imsha.rest;

import by.imsha.Application;
import by.imsha.repository.EntityWebhookRepository;
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
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "qa"})
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WebHookControllerMockMvcIT {
    @Container static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
    static final WireMockServer IDENTITY = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    static { IDENTITY.start(); }
    @AfterAll static void stopStub() { IDENTITY.stop(); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_webhook_mockmvc");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
    }
    @Autowired MockMvc mvc;
    @Autowired EntityWebhookRepository hooks;
    @Autowired CacheManager caches;
    @Autowired ObjectMapper json;
    private V1TestApi api;
    private String directory;
    @BeforeEach void isolate(TestInfo info) {
        directory = "webhook-mockmvc/" + info.getTestMethod().orElseThrow().getName() + "/";
        api = new V1TestApi(mvc, json);
        hooks.deleteAll();
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        IDENTITY.resetAll();
    }
    private String fixture(String name) throws Exception {
        return new ClassPathResource(directory + name).getContentAsString(StandardCharsets.UTF_8);
    }
    private String saved(String type) throws Exception {
        return saved(type, "minsk");
    }

    private String saved(String type, String key) throws Exception {
        return api.create("/hook/" + type, Map.of("key", key, "url", "https://example.org/callback"), status().isOk());
    }
    private void assertJson(MvcResult result, String id) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json").replace("${id}", id)));
    }
    @Test void createCity() throws Exception {
        MvcResult result = mvc.perform(post("/hook/city").header("X-Api-Key", "TEST_API_KEY")
                .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json"))).andExpect(status().isOk()).andReturn();
        String id = json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        assertJson(result, id);
        assertThat(api.getEntity("/hook", id).path("type").asText()).isEqualTo("city");
    }
    @Test void createParish() throws Exception {
        MvcResult result = mvc.perform(post("/hook/parish").header("X-Api-Key", "TEST_API_KEY")
                .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json"))).andExpect(status().isOk()).andReturn();
        String id = json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        assertJson(result, id);
        assertThat(api.getEntity("/hook", id).path("type").asText()).isEqualTo("parish");
    }
    @Test void getHook() throws Exception {
        String hook = saved("city");
        assertJson(mvc.perform(get("/hook/" + hook).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isOk()).andReturn(), hook);
    }
    @Test void listHooks() throws Exception {
        String hook = saved("city");
        assertJson(mvc.perform(get("/hook").param("page", "0").param("size", "1").header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isOk()).andReturn(), hook);
    }
    @Test void deleteHook() throws Exception {
        String hook = saved("city");
        MvcResult cached = mvc.perform(get("/hook/" + hook).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(cached.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("get-response.json").replace("${id}", hook)));
        assertJson(mvc.perform(delete("/hook/" + hook).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isOk()).andReturn(), hook);
        api.expectMissing("/hook", hook);
        mvc.perform(get("/hook/" + hook).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isNotFound());
    }

    @Test void missingKeyIsRejected() throws Exception {
        mvc.perform(post("/hook/city").header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.page("/hook").path("totalElements").asInt()).isZero();
    }

    @Test void missingUrlIsRejected() throws Exception {
        mvc.perform(post("/hook/parish").header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(api.page("/hook").path("totalElements").asInt()).isZero();
    }

    @Test void emptyKeyAndUrlAreAccepted() throws Exception {
        MvcResult result = mvc.perform(post("/hook/city").header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        String id = json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        JsonNode observed = api.getEntity("/hook", id);
        assertThat(observed.path("key").isTextual()).isTrue();
        assertThat(observed.path("key").asText()).isEmpty();
        assertThat(observed.path("url").isTextual()).isTrue();
        assertThat(observed.path("url").asText()).isEmpty();
    }

    @Test void duplicateKeyAndTypeIsRejected() throws Exception {
        String existing = saved("city");
        JsonNode original = api.getEntity("/hook", existing);
        mvc.perform(post("/hook/city").header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isInternalServerError());
        assertThat(api.page("/hook").path("content")).containsExactly(original);
    }

    @Test void sameKeyIsAcceptedForDifferentTypes() throws Exception {
        saved("city");
        mvc.perform(post("/hook/parish").header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk());
        assertThat(api.page("/hook").path("content")).extracting(hook -> hook.path("type").asText())
                .containsExactlyInAnyOrder("city", "parish");
    }

    @Test void missingHookReturnsNotFound() throws Exception {
        String id = "000000000000000000000000";
        mvc.perform(get("/hook/" + id).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/hook/" + id).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isNotFound());
        assertThat(api.page("/hook").path("totalElements").asInt()).isZero();
    }

    @Test void anonymousCannotListOrDelete() throws Exception {
        String existing = saved("city");
        mvc.perform(get("/hook")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/hook/" + existing)).andExpect(status().isUnauthorized());
        api.getEntity("/hook", existing);
    }

    @Test void anonymousCannotCreateHook() throws Exception {
        mvc.perform(post("/hook/city").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"minsk\",\"url\":\"https://example.org/callback\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(api.page("/hook").path("totalElements").asInt()).isZero();
    }

    @Test void listHooksPaginatesWithoutDuplicates() throws Exception {
        String one = saved("city", "minsk");
        String two = saved("city", "pinsk");
        JsonNode first = json.readTree(mvc.perform(get("/hook").param("page", "0").param("size", "1")
                .header("X-Api-Key", "TEST_API_KEY")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode second = json.readTree(mvc.perform(get("/hook").param("page", "1").param("size", "1")
                .header("X-Api-Key", "TEST_API_KEY")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode complete = json.readTree(mvc.perform(get("/hook").param("page", "0").param("size", "2")
                .header("X-Api-Key", "TEST_API_KEY")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(first.path("number").asInt()).isZero();
        assertThat(second.path("number").asInt()).isEqualTo(1);
        for (JsonNode page : new JsonNode[]{first, second}) {
            assertThat(page.path("size").asInt()).isEqualTo(1);
            assertThat(page.path("totalElements").asInt()).isEqualTo(2);
            assertThat(page.path("totalPages").asInt()).isEqualTo(2);
            assertThat(page.path("content").size()).isEqualTo(1);
        }
        assertThat(List.of(complete.path("content").get(0).path("id").asText(),
                        complete.path("content").get(1).path("id").asText()))
                .containsExactlyInAnyOrder(one, two);
        assertThat(List.of(one, two)).contains(first.path("content").get(0).path("id").asText(),
                second.path("content").get(0).path("id").asText());
    }
}
