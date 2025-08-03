package by.imsha.rest.passwordless.handler;

import by.imsha.rest.passwordless.exception.PasswordlessApiException;
import api_specification.by.imsha.server.fusionauth.public_client.api.FusionauthPublicApiClient;
import api_specification.by.imsha.server.fusionauth.public_client.model.SendCodeByEmailRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Обработчик запроса на аутентификацию по коду
 */
@Component
@RequiredArgsConstructor
@Validated
@Slf4j
public class LoginHandler {

    private final FusionauthPublicApiClient fusionauthPublicApiClient;

    public String handle(@Valid @NotNull(message = "Входные параметры обязательны для заполнения")
                         Input input) {
        try {
            log.info("[VERBOSE] Received code: '{}'", input.getCode());

            return fusionauthPublicApiClient.authByCode(
                            SendCodeByEmailRequest.builder()
                                    .code(input.getCode())
                                    .build()
                    ).getBody()
                    .getToken();
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
}
