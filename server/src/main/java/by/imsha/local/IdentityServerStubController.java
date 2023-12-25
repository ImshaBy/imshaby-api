package by.imsha.local;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Необходим для возможности тестирования всего проекта без поднятия identity server
 */
@RestController
@RequestMapping("/api/user")
@Profile("local")
@RequiredArgsConstructor
public class IdentityServerStubController {

    private final ObjectMapper objectMapper;
    private final Supplier<List<LocalComposeEnvironmentConfiguration.UserStub>> userStubsLoader;

    @GetMapping
    public ResponseEntity<JsonNode> getUserByJwt(@AuthenticationPrincipal Jwt jwt) {
        final String email = jwt.getClaim("email");
        final List<LocalComposeEnvironmentConfiguration.UserStub> userStubs = userStubsLoader.get();

        return userStubs.stream().filter(userStub -> Objects.equals(userStub.getEmail(), email)).findFirst()
                .map(userStub ->
                        {
                            try {
                                return ResponseEntity.ok(
                                        objectMapper.readTree(
                                                String.format(
                                                        GET_USER_RESPONSE_TEMPLATE,
                                                        escapeStringOrNullValue(userStub.getDefaultParish()),
                                                        escapeArrayOrEmptyValue(userStub.getParishes()),
                                                        escapeStringOrNullValue(userStub.getEmail()))
                                        )
                                );
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException("fail to load user data", e);
                            }
                        }
                )
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .build());
    }

    private static String escapeArrayOrEmptyValue(List<String> list) {
        return CollectionUtils.isEmpty(list) ? "" : list.stream().map(IdentityServerStubController::escapeStringOrNullValue).collect(Collectors.joining(","));
    }

    private static String escapeStringOrNullValue(String value) {
        return StringUtils.isBlank(value) ? "null" : String.format("\"%s\"", value);
    }

    private static final String GET_USER_RESPONSE_TEMPLATE = "{" +
            "\"user\":{" +
            "\"active\":true," +
            "\"connectorId\":\"e3306678-a53a-4964-9040-1c96f36dda72\"," +
            "\"data\":{" +
            "\"defaultParish\":%s," +
            "\"parishes\":[%s]" +
            "}," +
            "\"email\":%s," +
            "\"id\":\"5addf028-6d18-4059-85ad-83cca9e3d759\"," +
            "\"insertInstant\":1677930226014," +
            "\"lastLoginInstant\":1691927408033," +
            "\"lastUpdateInstant\":1685364781900," +
            "\"memberships\":[]," +
            "\"passwordChangeRequired\":false," +
            "\"passwordLastUpdateInstant\":1689057494814," +
            "\"preferredLanguages\":[]," +
            "\"registrations\":[" +
            "{" +
            "\"applicationId\":\"ab7868a4-9b1b-4cde-9cc6-72fa1bbe0b0f\"," +
            "\"data\":{}," +
            "\"id\":\"88077f99-36e4-476d-b447-3c47c20aca87\"," +
            "\"insertInstant\":1689057494368," +
            "\"lastLoginInstant\":1691927408033," +
            "\"lastUpdateInstant\":1689057494368," +
            "\"preferredLanguages\":[" +
            "\"be\"" +
            "]," +
            "\"roles\":[" +
            "\"Admin\"" +
            "]," +
            "\"timezone\":\"Europe/Minsk\"," +
            "\"tokens\":{}," +
            "\"username\":\"slongtong@mail.ru\"," +
            "\"usernameStatus\":\"ACTIVE\"," +
            "\"verified\":true" +
            "}" +
            "]," +
            "\"tenantId\":\"00862ea0-7531-31ee-7353-4a1e8b22498d\"," +
            "\"twoFactor\":{" +
            "\"methods\":[]," +
            "\"recoveryCodes\":[]" +
            "}," +
            "\"usernameStatus\":\"ACTIVE\"," +
            "\"verified\":true" +
            "}" +
            "}";

}
