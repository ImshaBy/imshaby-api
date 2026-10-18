package by.imsha.rest;

import by.imsha.Application;
import tools.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "qa"})
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PingControllerMockMvcIT {
    @Container
    static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
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
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_ping_mockmvc");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
        registry.add("sentry.dsn", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private void assertJson(org.springframework.test.web.servlet.MvcResult result, String path) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        String expected = new ClassPathResource("ping-mockmvc/" + path + "/response.json")
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                .isEqualTo(objectMapper.readTree(expected));
    }

    @Test
    void defaultLocaleIgnoresAcceptLanguage() throws Exception {
        mvc.perform(get("/").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andDo(result -> assertJson(result, "defaultLocaleIgnoresAcceptLanguage"));
    }

    @Test
    void localeCookieOverridesDefault() throws Exception {
        mvc.perform(get("/").cookie(new Cookie("i_lang", "en-US")))
                .andExpect(status().isOk())
                .andDo(result -> assertJson(result, "localeCookieOverridesDefault"));
    }

    @Test
    void sentryEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/sentry-test"))
                .andExpect(status().isUnauthorized())
                .andDo(result -> {
                    assertThat(result.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
                    assertThat(result.getResponse().getContentAsString()).isEmpty();
                    assertThat(result.getResponse().getContentType()).isNull();
                });
    }

    @Test
    void authenticatedSentryEndpointReturnsConfirmation() throws Exception {
        mvc.perform(get("/sentry-test").header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isOk())
                .andDo(result -> assertJson(result, "authenticatedSentryEndpointReturnsConfirmation"));
    }
}
