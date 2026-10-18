package by.imsha.meilisearch.reader;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class DefaultMeilisearchReaderIT {
    @Container
    static final GenericContainer<?> MEILI = new GenericContainer<>("getmeili/meilisearch:v1.12.1")
            .withExposedPorts(7700);

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void realIndexFiltersWeekAndReturnsNearestMassPerParish() throws Exception {
        request("POST", "/indexes", "{\"uid\":\"masses\",\"primaryKey\":\"recordId\"}");
        awaitTasks();
        request("PUT", "/indexes/masses/settings/filterable-attributes",
                "[\"dateTime\",\"city.id\",\"parish.id\",\"lang\",\"online\",\"rorate\"]");
        request("PUT", "/indexes/masses/settings/sortable-attributes", "[\"dateTime\"]");
        awaitTasks();
        request("POST", "/indexes/masses/documents", """
                [{"recordId":"a1","massId":"m1","dateTime":1736154000,"parish":{"id":"p1","state":"APPROVED","actual":true},"city":{"id":"c1"},"lang":"be","online":true,"rorate":false,"_geo":{"lat":53.9,"lng":27.5}},
                 {"recordId":"a2","massId":"m2","dateTime":1736157600,"parish":{"id":"p1","state":"APPROVED","actual":true},"city":{"id":"c1"},"lang":"be","online":true,"rorate":false,"_geo":{"lat":53.9,"lng":27.5}},
                 {"recordId":"a3","massId":"m3","dateTime":1736154000,"parish":{"id":"p2","state":"APPROVED","actual":true},"city":{"id":"c2"},"lang":"be","online":true,"rorate":false,"_geo":{"lat":53.9,"lng":27.5}}]
                """);
        awaitTasks();

        var reader = new DefaultMeilisearchReader(new Client(new Config(baseUrl(), null)), "masses", json);
        var filter = MassSearchFilter.builder().cityId("c1")
                .dateTimeFrom(OffsetDateTime.parse("2025-01-06T00:00:00+03:00"))
                .dateTimeTo(OffsetDateTime.parse("2025-01-06T23:59:59+03:00")).build();
        assertThat(reader.searchAllMasses(filter).hits()).extracting(item -> item.massId())
                .containsExactlyInAnyOrder("m1", "m2");
        var nearest = reader.searchNearestMasses(filter).hits();
        assertThat(nearest).hasSize(1);
        assertThat(nearest.getFirst().parish().id()).isEqualTo("p1");
        assertThat(nearest.getFirst().dateTime()).isEqualTo(LocalDateTime.of(2025, 1, 6, 12, 0));
    }

    private String baseUrl() {
        return "http://" + MEILI.getHost() + ":" + MEILI.getMappedPort(7700);
    }

    private String request(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json");
        var response = http.send(builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(path + ": " + response.body()).isBetween(200, 299);
        return response.body();
    }

    private void awaitTasks() throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            var response = http.send(HttpRequest.newBuilder(URI.create(baseUrl() + "/tasks?limit=100")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            var tasks = json.readTree(response.body()).path("results");
            boolean done = true;
            for (var task : tasks) {
                assertThat(task.path("status").asText()).isNotEqualTo("failed");
                done &= task.path("status").asText().equals("succeeded");
            }
            if (done) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Meilisearch tasks did not finish");
    }
}
