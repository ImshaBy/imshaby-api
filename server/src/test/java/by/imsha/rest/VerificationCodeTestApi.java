package by.imsha.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class VerificationCodeTestApi {
    private static final String INTERNAL_KEY = "TEST_INTERNAL_API_KEY";
    private final MockMvc mvc;
    private final ObjectMapper json;
    private final WireMockServer identity;

    public VerificationCodeTestApi(MockMvc mvc, ObjectMapper json, WireMockServer identity) {
        this.mvc = mvc;
        this.json = json;
        this.identity = identity;
    }

    public String requestCode(String email) throws Exception {
        identity.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathMatching("/api/email/send/.*"))
                .willReturn(okJson("{}")));
        mvc.perform(post("/api/auth/request-code").header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk());
        return deliveredCode();
    }

    public String deliveredCode() {
        var emails = identity.findAll(postRequestedFor(urlPathMatching("/api/email/send/.*")));
        assertThat(emails).hasSize(1);
        String code = json.readTree(emails.getFirst().getBodyAsString())
                .path("requestData").path("confirmationCode").asText();
        assertThat(code).matches("[0-9]{4}");
        return code;
    }

    private MvcResult verify(String email, String code) throws Exception {
        return mvc.perform(post("/api/auth/verify-code").header("X-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "confirmationCode", code))))
                .andExpect(status().isOk()).andReturn();
    }

    public boolean isValid(String email, String code) throws Exception {
        var body = json.readTree(verify(email, code).getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.path("valid").isBoolean()).isTrue();
        return body.path("valid").asBoolean();
    }
}
