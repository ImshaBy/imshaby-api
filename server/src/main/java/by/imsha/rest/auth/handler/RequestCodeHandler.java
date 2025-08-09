package by.imsha.rest.auth.handler;

import api_specification.by.imsha.common.fusionauth.secured_client.api.FusionauthApiClient;
import api_specification.by.imsha.common.fusionauth.secured_client.model.SendEmailResponse;
import by.imsha.properties.FusionauthProperties;
import by.imsha.rest.auth.exception.AuthException;
import by.imsha.rest.auth.mapper.FusionauthMapper;
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
public class RequestCodeHandler {

    private final FusionauthApiClient fusionauthApiClient;
    private final FusionauthProperties fusionauthProperties;
    private final ConfirmationCodeGenerator confirmationCodeGenerator;
    private final FusionauthMapper fusionauthMapper;

    public void handle(@Valid @NotNull(message = "Входные параметры обязательны для заполнения")
                       Input input) {

        try {
            String confirmationCode = confirmationCodeGenerator.generate(input.getEmail());

            SendEmailResponse response = fusionauthApiClient.sendEmail(
                    fusionauthProperties.getConfirmationCodeEmailTemplateId(),
                    fusionauthMapper.map(confirmationCode, input)
            ).getBody();

            log.info("Результат отправки кода подтверждения на email: {}", response);
        } catch (Exception exception) {
            throw new AuthException("Ошибка отправки кода подтверждения на email", exception);
        }
    }

    @Builder
    @Value
    public static class Input {

        /**
         * email пользователя
         */
        @NotBlank(message = "Адрес электронной почты пользователя")
        String email;
    }
}
