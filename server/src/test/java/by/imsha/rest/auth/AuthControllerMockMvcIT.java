package by.imsha.rest.auth;

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
class AuthControllerMockMvcIT {
    private static final String SEND = "/api/auth/request-code";
    private static final String VERIFY = "/api/auth/verify-code";
    private static final String INTERNAL_KEY = "TEST_INTERNAL_API_KEY";
    private static final String EMAIL = "person@example.org";
    @Container static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
    static final WireMockServer IDENTITY = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    static { IDENTITY.start(); }
    @AfterAll static void stopStub() { IDENTITY.stop(); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_auth_mockmvc");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
    }
    @Autowired MockMvc mvc;
    @Autowired CacheManager caches;
    @Autowired ObjectMapper json;
    private String directory;
    @BeforeEach void isolate(TestInfo info) {
        directory = "auth-mockmvc/" + info.getTestMethod().orElseThrow().getName() + "/";
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

    @Test void internalKeySendsVerificationEmail() throws Exception {
        IDENTITY.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/api/email/send/.*"))
                .willReturn(okJson("{}")));
        MvcResult result = mvc.perform(post(SEND).header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).isEmpty();
        IDENTITY.verify(1, postRequestedFor(urlPathMatching("/api/email/send/.*"))
                .withRequestBody(containing(EMAIL)));
        String code = caches.getCache("confirmationCodeCache").get(EMAIL, String.class);
        assertThat(code).isNotBlank();
        String sentBody = IDENTITY.findAll(postRequestedFor(urlPathMatching("/api/email/send/.*")))
                .get(0).getBodyAsString();
        assertThat(json.readTree(sentBody)).isEqualTo(json.readTree(fixture("email-request.json")
                .replace("${code}", code)));
    }

    @Test void ordinaryKeyCannotRequestCode() throws Exception {
        mvc.perform(post(SEND).header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isInternalServerError());
        IDENTITY.verify(0, postRequestedFor(urlPathMatching("/api/email/send/.*")));
    }

    @Test void anonymousCannotRequestOrVerifyCode() throws Exception {
        mvc.perform(post(SEND).contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(VERIFY).contentType(MediaType.APPLICATION_JSON).content(fixture("verify-request.json")))
                .andExpect(status().isUnauthorized());
    }

    @Test void missingCodeReturnsFalse() throws Exception {
        assertJson(mvc.perform(post(VERIFY).header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn());
    }

    @Test void matchingCodeCanBeVerifiedRepeatedly() throws Exception {
        caches.getCache("confirmationCodeCache").put(EMAIL, "1234");
        for (int attempt = 0; attempt < 2; attempt++) {
            assertJson(mvc.perform(post(VERIFY).header("X-Api-Key", INTERNAL_KEY)
                            .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                    .andExpect(status().isOk()).andReturn());
        }
    }

    @Test void wrongCodeReturnsFalse() throws Exception {
        caches.getCache("confirmationCodeCache").put(EMAIL, "1234");
        assertJson(mvc.perform(post(VERIFY).header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isOk()).andReturn());
    }

    @Test void missingEmailIsRejected() throws Exception {
        mvc.perform(post(SEND).header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        IDENTITY.verify(0, postRequestedFor(urlPathMatching("/api/email/send/.*")));
    }

    @Test void blankEmailIsRejected() throws Exception {
        mvc.perform(post(SEND).header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
        IDENTITY.verify(0, postRequestedFor(urlPathMatching("/api/email/send/.*")));
    }

    @Test void blankVerificationFieldsAreRejected() throws Exception {
        mvc.perform(post(VERIFY).header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("blank-email.json")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(VERIFY).header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("blank-code.json")))
                .andExpect(status().isBadRequest());
    }

    @Test void missingConfirmationCodeIsRejected() throws Exception {
        mvc.perform(post(VERIFY).header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isBadRequest());
    }

    @Test void ordinaryKeyCannotVerifyCode() throws Exception {
        caches.getCache("confirmationCodeCache").put(EMAIL, "1234");
        mvc.perform(post(VERIFY).header("X-Api-Key", "TEST_API_KEY")
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isInternalServerError());
    }

    @Test void emailProviderFailureReturnsServerError() throws Exception {
        IDENTITY.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/api/email/send/.*"))
                .willReturn(serverError()));
        mvc.perform(post(SEND).header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(fixture("request.json")))
                .andExpect(status().isInternalServerError());
        IDENTITY.verify(1, postRequestedFor(urlPathMatching("/api/email/send/.*")));
    }
}
