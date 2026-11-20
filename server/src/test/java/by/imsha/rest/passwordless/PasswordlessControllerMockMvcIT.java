package by.imsha.rest.passwordless;

import by.imsha.Application;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "qa"})
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordlessControllerMockMvcIT {
    private static final String START = "/api/passwordless/start";
    private static final String CODE = "/api/passwordless/code";
    private static final String LOGIN = "/api/passwordless/login";
    @Container static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
    static final WireMockServer IDENTITY = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    static { IDENTITY.start(); }
    @AfterAll static void stopStub() { IDENTITY.stop(); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_passwordless_mockmvc");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
    }
    @Autowired MockMvc mvc;
    @Autowired CacheManager caches;
    @Autowired ObjectMapper json;
    private String directory;
    @BeforeEach void isolate(TestInfo info) {
        directory = "passwordless-mockmvc/" + info.getTestMethod().orElseThrow().getName() + "/";
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        IDENTITY.resetAll();
    }
    private String fixture(String name) throws Exception {
        return new ClassPathResource(directory + name).getContentAsString(StandardCharsets.UTF_8);
    }
    private void assertJson(MvcResult result) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(json.readTree(fixture("response.json")));
    }
    private void assertFusionAuthBody(String path, String fixtureName) throws Exception {
        String body = IDENTITY.findAll(postRequestedFor(urlEqualTo(path))).get(0).getBodyAsString();
        assertThat(json.readTree(body)).isEqualTo(json.readTree(fixture(fixtureName)));
    }
    private void stubStart() {
        IDENTITY.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/passwordless/start"))
                .willReturn(okJson("{\"code\":\"123456789\"}")));
    }

    @Test void publicStartSendsCodeByEmail() throws Exception {
        stubStart();
        IDENTITY.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/passwordless/send"))
                .willReturn(ok()));
        MvcResult result = mvc.perform(post(START).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json"))).andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).isEmpty();
        IDENTITY.verify(1, postRequestedFor(urlEqualTo("/api/passwordless/start"))
                .withRequestBody(containing("person@example.org")));
        IDENTITY.verify(1, postRequestedFor(urlEqualTo("/api/passwordless/send"))
                .withRequestBody(containing("123456789")));
        assertFusionAuthBody("/api/passwordless/start", "start-request.json");
        assertFusionAuthBody("/api/passwordless/send", "send-request.json");
    }

    @Test void internalCodeReturnsGeneratedCodeWithoutSendingEmail() throws Exception {
        stubStart();
        assertJson(mvc.perform(post(CODE).header("X-Api-Key", "TEST_INTERNAL_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn());
        IDENTITY.verify(1, postRequestedFor(urlEqualTo("/api/passwordless/start")));
        IDENTITY.verify(0, postRequestedFor(urlEqualTo("/api/passwordless/send")));
        assertFusionAuthBody("/api/passwordless/start", "start-request.json");
    }

    @Test void publicLoginReturnsToken() throws Exception {
        IDENTITY.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/passwordless/login"))
                .willReturn(okJson("{\"token\":\"token-1\"}")));
        assertJson(mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn());
        IDENTITY.verify(1, postRequestedFor(urlEqualTo("/api/passwordless/login"))
                .withRequestBody(containing("123456789")));
        assertFusionAuthBody("/api/passwordless/login", "login-request.json");
    }

    @Test void unknownApplicationIsRejectedBeforeCallingFusionAuth() throws Exception {
        mvc.perform(post(START).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        IDENTITY.verify(0, postRequestedFor(urlEqualTo("/api/passwordless/start")));
    }

    @Test void missingEmailIsRejected() throws Exception {
        mvc.perform(post(START).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        IDENTITY.verify(0, postRequestedFor(urlEqualTo("/api/passwordless/start")));
    }

    @Test void missingLoginCodeIsRejected() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        IDENTITY.verify(0, postRequestedFor(urlEqualTo("/api/passwordless/login")));
    }

    @Test void blankStartEmailIsRejected() throws Exception {
        mvc.perform(post(START).contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        IDENTITY.verify(0, postRequestedFor(urlEqualTo("/api/passwordless/start")));
    }

    @Test void blankInternalCodeEmailIsRejected() throws Exception {
        mvc.perform(post(CODE).header("X-Api-Key", "TEST_INTERNAL_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        IDENTITY.verify(0, postRequestedFor(urlEqualTo("/api/passwordless/start")));
    }

    @Test void blankLoginCodeIsRejected() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        IDENTITY.verify(0, postRequestedFor(urlEqualTo("/api/passwordless/login")));
    }

    @Test void anonymousCannotRequestInternalCode() throws Exception {
        mvc.perform(post(CODE).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isUnauthorized());
        IDENTITY.verify(0, postRequestedFor(urlEqualTo("/api/passwordless/start")));
    }

    @Test void ordinaryKeyCannotRequestInternalCode() throws Exception {
        mvc.perform(post(CODE).header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isInternalServerError());
        IDENTITY.verify(0, postRequestedFor(urlEqualTo("/api/passwordless/start")));
    }

    @Test void fusionAuthStartFailureReturnsEmptySuccess() throws Exception {
        IDENTITY.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/passwordless/start"))
                .willReturn(serverError()));
        MvcResult result = mvc.perform(post(START).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).isEmpty();
        IDENTITY.verify(1, postRequestedFor(urlEqualTo("/api/passwordless/start")));
        IDENTITY.verify(0, postRequestedFor(urlEqualTo("/api/passwordless/send")));
    }

    @Test void fusionAuthLoginFailureReturnsServerError() throws Exception {
        IDENTITY.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/passwordless/login"))
                .willReturn(serverError()));
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content(fixture("request.json")))
                .andExpect(status().isInternalServerError());
    }
}
