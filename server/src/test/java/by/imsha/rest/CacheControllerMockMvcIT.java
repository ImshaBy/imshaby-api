package by.imsha.rest;

import by.imsha.Application;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = Application.class)
@Import(FixedClockTestConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "qa"})
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CacheControllerMockMvcIT {
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
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_cache_mockmvc");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
    }

    @Autowired MockMvc mvc;
    @Autowired CacheManager cacheManager;
    @Autowired ObjectMapper objectMapper;
    private VerificationCodeTestApi verification;
    private static final String EMAIL = "person@example.org";

    @BeforeEach
    void isolate() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        IDENTITY.resetAll();
        verification = new VerificationCodeTestApi(mvc, objectMapper, IDENTITY);
    }

    @Test
    void internalKeyClearsCache() throws Exception {
        String code = verification.requestCode(EMAIL);
        assertThat(verification.isValid(EMAIL, code)).isTrue();
        mvc.perform(post("/api/cache/clear").header("X-Api-Key", "TEST_INTERNAL_API_KEY"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    assertThat(result.getResponse().getContentAsString()).isEmpty();
                    assertThat(result.getResponse().getContentType()).isNull();
                });
        assertThat(verification.isValid(EMAIL, code)).isFalse();
    }

    @Test
    void ordinaryKeyCannotClearCache() throws Exception {
        String code = verification.requestCode(EMAIL);
        assertThat(verification.isValid(EMAIL, code)).isTrue();
        mvc.perform(post("/api/cache/clear").header("X-Api-Key", "TEST_API_KEY"))
                .andExpect(status().isInternalServerError())
                .andDo(result -> {
                    assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
                    String expected = new ClassPathResource("cache-mockmvc/ordinaryKeyCannotClearCache/response.json")
                            .getContentAsString(StandardCharsets.UTF_8);
                    assertThat(objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)))
                            .isEqualTo(objectMapper.readTree(expected));
                });
        assertThat(verification.isValid(EMAIL, code)).isTrue();
    }

    @Test
    void anonymousCannotClearCache() throws Exception {
        String code = verification.requestCode(EMAIL);
        assertThat(verification.isValid(EMAIL, code)).isTrue();
        mvc.perform(post("/api/cache/clear"))
                .andExpect(status().isUnauthorized())
                .andDo(result -> assertThat(result.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer"));
        assertThat(verification.isValid(EMAIL, code)).isTrue();
    }
}
