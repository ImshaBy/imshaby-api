package by.imsha.rest;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class V1TestApi {
    static final String CITIES = "/api/cities";
    static final String PARISHES = "/api/parish";
    static final String MASSES = "/api/mass";
    private static final String KEY = "TEST_API_KEY";

    private final MockMvc mvc;
    private final ObjectMapper json;

    V1TestApi(MockMvc mvc, ObjectMapper json) {
        this.mvc = mvc;
        this.json = json;
    }

    String create(String path, Map<String, ?> fields) throws Exception {
        return create(path, fields, status().isCreated());
    }

    String create(String path, Map<String, ?> fields, ResultMatcher expectedStatus) throws Exception {
        JsonNode created = body(mvc.perform(post(path).header("X-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(fields)))
                .andExpect(expectedStatus).andReturn());
        String id = created.path("id").asText();
        assertThat(id).matches("[0-9a-f]{24}");
        return id;
    }

    String city(String key, String name) throws Exception {
        return create(CITIES, Map.of("key", key, "name", name));
    }

    String parish(String state, Map<String, ?> fields) throws Exception {
        var request = new LinkedHashMap<String, Object>(fields);
        request.put("state", "INITIAL");
        String id = create(PARISHES, request);
        if (!state.equals("INITIAL")) {
            mvc.perform(put(PARISHES + "/" + id + "/state").header("X-Api-Key", KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("state", state))))
                    .andExpect(status().isOk());
        }
        return id;
    }

    JsonNode getEntity(String path, String id) throws Exception {
        return body(mvc.perform(get(path + "/" + id).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn());
    }

    void expectMissing(String path, String id) throws Exception {
        mvc.perform(get(path + "/" + id).header("X-Api-Key", KEY)).andExpect(status().isNotFound());
    }

    JsonNode filtered(String path) throws Exception {
        JsonNode result = body(mvc.perform(get(path).header("X-Api-Key", KEY)
                        .param("filter", "id!=000000000000000000000000").param("limit", "100"))
                .andExpect(status().isOk()).andReturn());
        assertThat(result.isArray()).isTrue();
        return result;
    }

    JsonNode page(String path) throws Exception {
        JsonNode result = body(mvc.perform(get(path).header("X-Api-Key", KEY))
                .andExpect(status().isOk()).andReturn());
        assertThat(result.path("content").isArray()).isTrue();
        return result;
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}


