package by.imsha.rest.auth.handler;

import by.imsha.properties.AuthProperties;
import by.imsha.rest.auth.exception.AuthException;
import by.imsha.server.api_specification.fusionauth.secured_client.api.FusionauthApiClient;
import by.imsha.server.api_specification.fusionauth.secured_client.model.Address;
import by.imsha.server.api_specification.fusionauth.secured_client.model.SendConfirmationCode;
import by.imsha.server.api_specification.fusionauth.secured_client.model.SendEmailRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Component
@RequiredArgsConstructor
@Validated
@Slf4j
public class RequestCodeHandler {

    private final FusionauthApiClient fusionauthApiClient;
    private final AuthProperties authProperties;
    private final ConfirmationCodeGenerator confirmationCodeGenerator;

    public void handle(@Valid @NotNull(message = "Входные параметры обязательны для заполнения")
                       Input input) {

        try {
            Void body = fusionauthApiClient.sendEmail(
                    authProperties.getConfirmationCodeEmailTemplateId(),
                    SendEmailRequest.builder()
                            .requestData(SendConfirmationCode.builder()
                                    .confirmationCode(confirmationCodeGenerator.generateCode(input.getEmail()))
                                    .build())
                            .toAddresses(
                                    List.of(Address.builder()
                                            .address(input.getEmail())
                                            .build())
                            )
                            .build()
            ).getBody();
            //TODO описать поля ответа и залогировать ответ
            log.info("Результат отправки кода подтверждения на email: {}", body);
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
