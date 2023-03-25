package by.imsha.rest.passwordless.handler;

import by.imsha.properties.PasswordlessApiProperties;
import by.imsha.rest.passwordless.exception.PasswordlessApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Обработчик запроса на аутентификацию по коду
 */
@Component
@RequiredArgsConstructor
@Validated
@Slf4j
public class LoginHandler {

    private final PasswordlessApiProperties passwordlessApiProperties;
    private final RestTemplate passwordlessPublicRestTemplate;

    public String handle(@Valid @NotNull(message = "Входные параметры обязательны для заполнения")
                                 Input input) {
        try {
            ResponseBody response = passwordlessPublicRestTemplate.postForObject(
                    passwordlessApiProperties.getUri().getLogin(),
                    RequestBody.builder()
                            .code(input.getCode())
                            .build(),
                    ResponseBody.class
            );

            return response.getToken();
        } catch (Exception exception) {
            throw new PasswordlessApiException("Ошибка при получении токена через Passwordless API", true, exception);
        }
    }

    @Builder
    @Value
    public static class Input {
        /**
         * Уникальный код, отправленный пользователю по email, необходимый для завершения входа в систему
         */
        @NotBlank(message = "Уникальный код, отправленный пользователю по email не должен быть пустым")
        String code;
        /**
         * TODO Вопрос на развитие: возможно захотим валидировать секрет в запросе кода и ответе на login
         */
        String stateSecret;
    }

    @Builder
    @Value
    private static class RequestBody {
        /**
         * Уникальный код, необходимый для завершения входа в систему
         */
        String code;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ResponseBody {

        /**
         * JWT токен
         */
        private String token;
    }
}
