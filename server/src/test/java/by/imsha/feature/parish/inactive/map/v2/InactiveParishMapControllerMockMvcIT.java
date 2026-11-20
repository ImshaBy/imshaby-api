package by.imsha.feature.parish.inactive.map.v2;

import by.imsha.Application;
import by.imsha.domain.Coordinate;
import by.imsha.domain.LocalizedParish;
import by.imsha.domain.Parish;
import by.imsha.repository.ParishRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "qa"})
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InactiveParishMapControllerMockMvcIT {
    @Container static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7.0").withExposedPorts(27017);
    static final WireMockServer IDENTITY = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    static { IDENTITY.start(); }
    @AfterAll static void stopStub() { IDENTITY.stop(); }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/imshaby_inactive_map_mockmvc");
        registry.add("fusionauth.url", IDENTITY::baseUrl);
        registry.add("fusionauthPublic.url", IDENTITY::baseUrl);
    }

    @Autowired MockMvc mvc;
    @Autowired ParishRepository parishes;
    @Autowired CacheManager caches;
    @Autowired ObjectMapper json;

    @BeforeEach void isolate() {
        parishes.deleteAll();
        caches.getCacheNames().forEach(name -> caches.getCache(name).clear());
        IDENTITY.resetAll();
    }

    private Parish save(String id, Parish.State state, String name, boolean withGeo) {
        Parish parish = new Parish();
        parish.setId(id);
        parish.setName(name);
        parish.setKey(id);
        parish.setCityId("city-1");
        parish.setState(state);
        if (withGeo) parish.setGps(new Coordinate(53.9f, 27.57f));
        return parishes.save(parish);
    }

    @Test void returnsOnlyInactiveStatesWithLocalizedNamesAndCoordinates() throws Exception {
        // Update this list deliberately when Parish.State changes, then review the response fixture.
        assertThat(EnumSet.allOf(Parish.State.class)).containsExactlyInAnyOrder(
                Parish.State.INITIAL, Parish.State.PENDING, Parish.State.APPROVED, Parish.State.DISABLED);
        Parish initial = save("initial", Parish.State.INITIAL, "Initial", true);
        LocalizedParish translated = new LocalizedParish("en-US", initial.getId());
        translated.setName("Translated initial");
        initial.getLocalizedInfo().put("en-US", translated);
        parishes.save(initial);
        save("pending", Parish.State.PENDING, "Pending", false);
        save("disabled", Parish.State.DISABLED, "Disabled", true);
        save("approved", Parish.State.APPROVED, "Approved", true);

        JsonNode actual = json.readTree(mvc.perform(get("/api/v2/parishes/inactive/map")
                        .param("lang", "en-US"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode expected = json.readTree(new ClassPathResource(
                "feature/parish/inactive/map/v2/returnsOnlyInactiveStatesWithLocalizedNamesAndCoordinates/response.json")
                .getContentAsString(StandardCharsets.UTF_8));
        assertThat(actual.isArray()).isTrue();
        assertThat(actual.size()).isEqualTo(expected.size());
        java.util.List<JsonNode> actualItems = new java.util.ArrayList<>();
        java.util.List<JsonNode> expectedItems = new java.util.ArrayList<>();
        actual.forEach(actualItems::add);
        expected.forEach(expectedItems::add);
        assertThat(actualItems).containsExactlyInAnyOrderElementsOf(expectedItems);
    }
}
