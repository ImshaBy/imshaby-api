package by.imsha.server.rest.passwordless.handler;

import by.imsha.server.properties.PasswordlessApiProperties;
import by.imsha.server.rest.passwordless.exception.PasswordlessApiException;
import by.imsha.server.rest.passwordless.send.CodeSender;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Validated
@Slf4j
public class StartHandler {

    private final PasswordlessApiProperties passwordlessApiProperties;
    private final RestTemplate passwordlessSecureRestTemplate;
    private final CodeSender defaultCodeSender;

    public void handle(@Valid @NotNull(message = "Входные параметры обязательны для заполнения") final Input input) {
        handle(input, defaultCodeSender);
    }

    public void handle(@Valid @NotNull(message = "Входные параметры обязательны для заполнения") final Input input,
                       @NotNull final CodeSender codeSender) {
        final String requestURI = passwordlessApiProperties.getUri().getStart();

        log.info("Request uri: '{}', data: '{}'", requestURI, input);

        try {
            final ResponseBody response = passwordlessSecureRestTemplate.postForObject(
                    requestURI,
                    RequestBody.builder()
                            .loginId(input.getLoginId())
                            .applicationId(input.getApplicationId())
                            .build(),
                    ResponseBody.class
            );

            codeSender.send(input.getLoginId(), response.getCode());
        } catch (PasswordlessApiException passwordlessApiException) {
            throw passwordlessApiException;
        } catch (Exception exception) {
            throw new PasswordlessApiException("Ошибка инициализации процесса беспарольного входа",
                    false, exception);
        }
    }

    @Builder
    @Value
    public static class Input {
        /**
         * Уникальный идентификатор приложения, в которое запрашиваем вход
         */
        @NotBlank(message = "Идентификатор приложения не должен быть пустым")
        String applicationId;
        /**
         * Идентификатор пользователя для логина. Может быть либо email либо username.
         */
        @NotBlank(message = "Идентификатор пользователя (email) не должен быть пустым")
        String loginId;
        /**
         * TODO Вопрос на развитие: возможно захотим валидировать секрет в запросе кода и ответе на login
         */
        String stateSecret;
    }

    @Builder
    @Value
    private static class RequestBody {
        /**
         * Уникальный идентификатор приложения, в которое запрашиваем вход
         */
        @NotBlank(message = "Идентификатор приложения не должен быть пустым")
        String applicationId;
        /**
         * Идентификатор пользователя для логина. Может быть либо email либо username.
         */
        @NotBlank(message = "Идентификатор пользователя (email) не должен быть пустым")
        String loginId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ResponseBody {
        /**
         * Уникальный код, необходимый для завершения входа в систему
         */
        private String code;
    }
}
