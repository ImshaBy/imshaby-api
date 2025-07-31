package by.imsha.local;

import by.imsha.rest.passwordless.exception.PasswordlessApiException;
import by.imsha.rest.passwordless.handler.LoginHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Заглушки, необходимые для тестирования всего проекта без identity server
 */
@Configuration
@Profile("local")
public class LocalComposeEnvironmentConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
        final MappedJwtClaimSetConverter claimSetConverter = MappedJwtClaimSetConverter
                .withDefaults(Collections.emptyMap());

        return token -> {
            try {
                JWT parsedJwt = JWTParser.parse(token);
                JWTClaimsSet jwtClaimsSet = parsedJwt.getJWTClaimsSet();
                Map<String, Object> headers = new LinkedHashMap<>(parsedJwt.getHeader().toJSONObject());
                Map<String, Object> claims = claimSetConverter.convert(jwtClaimsSet.getClaims());

                return Jwt.withTokenValue(token)
                        .headers((h) -> h.putAll(headers))
                        .claims((c) -> c.putAll(claims))
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("jwt decode failed", e);
            }
        };
    }

    @Bean
    Supplier<List<UserStub>> userStubsLoader(@org.springframework.beans.factory.annotation.Value("${local.user.stub.folder}") String stubFolderLocation,
                                             ObjectMapper objectMapper) {
        if (StringUtils.isBlank(stubFolderLocation)) {
            throw new RuntimeException("Stub file location is required");
        }
        final File file = new File(stubFolderLocation, "stub.json");
        if (!file.exists()) {
            try {
                file.createNewFile();
                saveUserStubs(file, Collections.singletonList(UserStub.builder()
                        .code("example")
                        .email("example@mail.ru")
                        .defaultParish("gorka")
                        .parishes(Arrays.asList("gorka1", "gorka2"))
                        .build()), objectMapper);
            } catch (Exception e) {
                throw new RuntimeException("Can't create 'stub.json' file", e);
            }
        }
        return () -> {
            try {
                return loadUserStubs(file, objectMapper);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load or create file", e);
            }
        };

    }

    /**
     * Переопределяем обработчик логина, работу со стабами
     */
    @Bean
    @Primary
    LoginHandler localLoginHandler(Supplier<List<UserStub>> userStubsLoader) {
        return new LoginHandler(null) {
            @Override
            public String handle(Input input) {
                final String code = input.getCode();

                return userStubsLoader.get().stream()
                        .filter(userStub -> Objects.equals(userStub.getCode(), code))
                        .findFirst()
                        .map(userStub ->
                                String.join(".",
                                        base64Encode(jwtHeader),
                                        base64Encode(String.format(jwtPayload, userStub.getEmail())),
                                        "")
                        )
                        .orElseThrow(() -> new PasswordlessApiException("not found", true));
            }
        };
    }

    private static void saveUserStubs(File file, List<UserStub> userStubs, ObjectMapper objectMapper) {
        try {
            Files.write(file.toPath(), objectMapper.writeValueAsBytes(userStubs));
        } catch (IOException e) {
            new RuntimeException("Failed to save data");
        }
    }

    private static List<UserStub> loadUserStubs(File file, ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(file, new TypeReference<List<UserStub>>() {
        });
    }

    private static String base64Encode(String string) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    private static final String jwtHeader = "{" +
            "\"alg\":\"none\"," +
            "\"typ\":\"JWT\"" +
            "}";
    private static final String jwtPayload = "{" +
            "\"aud\":\"ab7868a4-9b1b-4cde-9cc6-72fa1bbe0b0f\"," +
            "\"exp\":1691931008," +
            "\"iat\":1691927408," +
            "\"iss\":\"acme.com\"," +
            "\"sub\":\"5addf028-6d18-4059-85ad-83cca9e3d759\"," +
            "\"jti\":\"e46ea6b4-7012-4b75-829b-f58e4c9f8fce\"," +
            "\"authenticationType\":\"PASSWORDLESS\"," +
            "\"email\":\"%s\"," +
            "\"email_verified\":true," +
            "\"applicationId\":\"ab7868a4-9b1b-4cde-9cc6-72fa1bbe0b0f\"," +
            "\"roles\":[" +
            "\"Admin\"" +
            "]," +
            "\"auth_time\":1691927408," +
            "\"tid\":\"00862ea0-7531-31ee-7353-4a1e8b22498d\"" +
            "}";

    @Value
    @Builder
    @Jacksonized
    public static class UserStub {

        String email;
        String code;
        String defaultParish;
        List<String> parishes;
    }
}
