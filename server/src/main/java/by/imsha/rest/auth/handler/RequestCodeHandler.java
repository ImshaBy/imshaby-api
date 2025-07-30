package by.imsha.rest.auth.handler;

import by.imsha.properties.AuthProperties;
import by.imsha.rest.auth.exception.AuthException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Validated
@Slf4j
public class RequestCodeHandler {

    private final RestTemplate passwordlessSecureRestTemplate;
    private final AuthProperties authProperties;
    private final ConfirmationCodeGenerator confirmationCodeGenerator;

    public void handle(@Valid @NotNull(message = "Входные параметры обязательны для заполнения")
                       Input input) {

        try {
            String response = passwordlessSecureRestTemplate.postForObject(
                    authProperties.getUri().getSendEmail() + "/" + authProperties.getConfirmationCodeEmailTemplateId(),
                    RequestBody.builder()
                            .requestData(
                                    RequestBody.RequestData.builder()
                                            .confirmationCode(confirmationCodeGenerator.generateCode(input.getEmail()))
                                            .build()
                            )
                            .toAddresses(
                                    List.of(
                                            RequestBody.AddressContainer.builder()
                                                    .address(input.getEmail())
                                                    .build()
                                    )
                            )
                            .build(),
                    String.class
            );

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

    @Builder
    @Value
    private static class RequestBody {

        /**
         * Данные для подстановки в шаблон
         */
        RequestData requestData;

        /**
         * Адреса, на которые необходимо отправить письмо
         */
        List<AddressContainer> toAddresses;

        @Builder
        @Value
        public static class RequestData {

            /**
             * Код подтверждения
             */
            String confirmationCode;
        }

        @Builder
        @Value
        public static class AddressContainer {

            /**
             * email адрес
             */
            String address;
        }
    }
}
