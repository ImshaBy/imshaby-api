package by.imsha.rest.passwordless.handler;

import by.imsha.rest.passwordless.exception.PasswordlessApiException;
import by.imsha.rest.passwordless.mapper.FusionauthMapper;
import by.imsha.rest.passwordless.send.CodeSender;
import api_specification.by.imsha.server.fusionauth.secured_client.api.FusionauthApiClient;
import api_specification.by.imsha.server.fusionauth.secured_client.model.StartPasswordlessLoginRequest;
import api_specification.by.imsha.server.fusionauth.secured_client.model.StartPasswordlessLoginResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@RequiredArgsConstructor
@Validated
@Slf4j
public class StartHandler {

    private final FusionauthApiClient fusionauthApiClient;
    private final CodeSender defaultCodeSender;
    private final FusionauthMapper fusionauthMapper;

    public void handle(@Valid @NotNull(message = "Входные параметры обязательны для заполнения") final Input input) {
        handle(input, defaultCodeSender);
    }

    public void handle(@Valid @NotNull(message = "Входные параметры обязательны для заполнения") final Input input,
                       @NotNull final CodeSender codeSender) {
        log.info("Start passwordless login. Data: '{}'", input);

        try {
            StartPasswordlessLoginResponse response = fusionauthApiClient.startPasswordlessLogin(
                    fusionauthMapper.map(input)
            ).getBody();

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
}
