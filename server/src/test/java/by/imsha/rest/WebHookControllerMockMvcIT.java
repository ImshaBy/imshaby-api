package by.imsha.rest;

import by.imsha.Application;
import by.imsha.domain.EntityWebhook;
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
    private String directory;
    @BeforeEach void isolate(TestInfo info) {
        directory = "webhook-mockmvc/" + info.getTestMethod().orElseThrow().getName() + "/";
        hooks.deleteAll();
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        IDENTITY.resetAll();
    }
    private String fixture(String name) throws Exception {
        return new ClassPathResource(directory + name).getContentAsString(StandardCharsets.UTF_8);
    }
    private EntityWebhook saved(String type) {
        return hooks.save(EntityWebhook.builder().type(type).key("minsk").url("https://example.org/callback").build());
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
        assertThat(hooks.findById(id).orElseThrow().getType()).isEqualTo("city");
    }
    @Test void createParish() throws Exception {
        MvcResult result = mvc.perform(post("/hook/parish").header("X-Api-Key", "TEST_API_KEY")
                .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json"))).andExpect(status().isOk()).andReturn();
        String id = json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        assertJson(result, id);
        assertThat(hooks.findById(id).orElseThrow().getType()).isEqualTo("parish");
    }
    @Test void getHook() throws Exception {
        EntityWebhook hook = saved("city");
        assertJson(mvc.perform(get("/hook/" + hook.getId()).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isOk()).andReturn(), hook.getId());
    }
    @Test void listHooks() throws Exception {
        EntityWebhook hook = saved("city");
        assertJson(mvc.perform(get("/hook").param("page", "0").param("size", "1").header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isOk()).andReturn(), hook.getId());
    }
    @Test void deleteHook() throws Exception {
        EntityWebhook hook = saved("city");
        MvcResult cached = mvc.perform(get("/hook/" + hook.getId()).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(cached.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("get-response.json").replace("${id}", hook.getId())));
        assertJson(mvc.perform(delete("/hook/" + hook.getId()).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isOk()).andReturn(), hook.getId());
        assertThat(hooks.findById(hook.getId())).isEmpty();
        mvc.perform(get("/hook/" + hook.getId()).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isNotFound());
    }

    @Test void missingKeyIsRejected() throws Exception {
        mvc.perform(post("/hook/city").header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(hooks.count()).isZero();
    }

    @Test void missingUrlIsRejected() throws Exception {
        mvc.perform(post("/hook/parish").header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        assertThat(hooks.count()).isZero();
    }

    @Test void emptyKeyAndUrlAreAccepted() throws Exception {
        MvcResult result = mvc.perform(post("/hook/city").header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        String id = json.readTree(result.getResponse().getContentAsString()).path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        assertThat(hooks.findById(id).orElseThrow().getKey()).isEmpty();
        assertThat(hooks.findById(id).orElseThrow().getUrl()).isEmpty();
    }

    @Test void duplicateKeyAndTypeIsRejected() throws Exception {
        EntityWebhook existing = saved("city");
        mvc.perform(post("/hook/city").header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isInternalServerError());
        assertThat(hooks.findAll()).containsExactly(existing);
    }

    @Test void sameKeyIsAcceptedForDifferentTypes() throws Exception {
        saved("city");
        mvc.perform(post("/hook/parish").header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk());
        assertThat(hooks.findAll()).extracting(EntityWebhook::getType)
                .containsExactlyInAnyOrder("city", "parish");
    }

    @Test void missingHookReturnsNotFound() throws Exception {
        String id = "000000000000000000000000";
        mvc.perform(get("/hook/" + id).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/hook/" + id).header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isNotFound());
        assertThat(hooks.count()).isZero();
    }

    @Test void anonymousCannotListOrDelete() throws Exception {
        EntityWebhook existing = saved("city");
        mvc.perform(get("/hook")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/hook/" + existing.getId())).andExpect(status().isUnauthorized());
        assertThat(hooks.findById(existing.getId())).isPresent();
    }

    @Test void anonymousCannotCreateHook() throws Exception {
        mvc.perform(post("/hook/city").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"minsk\",\"url\":\"https://example.org/callback\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(hooks.count()).isZero();
    }

    @Test void listHooksPaginatesWithoutDuplicates() throws Exception {
        EntityWebhook one = hooks.save(EntityWebhook.builder().type("city").key("minsk").url("https://example.org/callback").build());
        EntityWebhook two = hooks.save(EntityWebhook.builder().type("city").key("pinsk").url("https://example.org/callback").build());
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
                .containsExactlyInAnyOrder(one.getId(), two.getId());
        assertThat(List.of(one.getId(), two.getId())).contains(first.path("content").get(0).path("id").asText(),
                second.path("content").get(0).path("id").asText());
    }
}
