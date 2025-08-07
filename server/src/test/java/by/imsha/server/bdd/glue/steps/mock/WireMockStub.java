package by.imsha.server.bdd.glue.steps.mock;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import io.cucumber.datatable.DataTable;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public enum WireMockStub {

    FUSIONAUTH_PASSWORDLESS_START("Генерация кода аутентификации в FusionAuth",
            dataTable -> {

                Map<String, String> properties = dataTable.asMap();

                String fusionAuthApiKey = properties.get("api-key");
                String email = properties.get("email");
                String applicationId = properties.get("идентификатор приложения");
                String authenticationCode = properties.get("код аутентификации");

                String requestBody = """
                        {
                            "applicationId": "%s",
                            "loginId": "%s"
                        }
                        """.formatted(applicationId, email);
                String responseBody = """
                        {
                            "code": "%s"
                        }
                        """.formatted(authenticationCode);

                return WireMock.post(WireMock.urlEqualTo("/fusion-auth/api/passwordless/start"))
                        .withHeader("Authorization", new EqualToPattern(fusionAuthApiKey))
                        .withRequestBody(new EqualToJsonPattern(requestBody, true, false))
                        .willReturn(WireMock.jsonResponse(responseBody, 200));
            }),
    FUSIONAUTH_PASSWORDLESS_SEND("Отправка кода аутентификации на почту в FusionAuth",
            dataTable -> {

                Map<String, String> properties = dataTable.asMap();

                String code = properties.get("код аутентификации");

                String requestBody = """
                        {
                            "code": "%s"
                        }
                        """.formatted(code);

                return WireMock.post(WireMock.urlEqualTo("/fusion-auth/api/passwordless/send"))
                        .withRequestBody(new EqualToJsonPattern(requestBody, true, false))
                        .willReturn(WireMock.aResponse().withStatus(200));
            }),
    FUSIONAUTH_PASSWORDLESS_LOGIN("Обмен кода аутентификации на токен в FusionAuth",
            dataTable -> {

                Map<String, String> properties = dataTable.asMap();

                String authenticationCode = properties.get("код аутентификации");
                String token = properties.get("токен");

                String requestBody = """
                        {
                            "code": "%s"
                        }
                        """.formatted(authenticationCode);
                String responseBody = """
                        {
                            "token": "%s"
                        }
                        """.formatted(token);

                return WireMock.post(WireMock.urlEqualTo("/fusion-auth/api/passwordless/login"))
                        .withRequestBody(new EqualToJsonPattern(requestBody, true, false))
                        .willReturn(WireMock.jsonResponse(responseBody, 200));
            }),
    ;

    private final String name;
    private final Function<DataTable, MappingBuilder> mockBuilder;

    public MappingBuilder getMapping(DataTable dataTable) {
        return mockBuilder.apply(dataTable);
    }

    public static WireMockStub getByName(String name) {

        return Arrays.stream(WireMockStub.values())
                .filter(wireMockStub -> wireMockStub.name.equals(name))
                .findFirst()
                .orElseThrow();
    }
}
