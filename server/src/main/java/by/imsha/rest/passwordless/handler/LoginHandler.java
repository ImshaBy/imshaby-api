package by.imsha.rest.passwordless.handler;

import by.imsha.rest.passwordless.exception.PasswordlessApiException;
import api_specification.by.imsha.common.fusionauth.public_client.api.FusionauthPublicApiClient;
import api_specification.by.imsha.common.fusionauth.public_client.model.SendCodeByEmailRequest;
import by.imsha.rest.passwordless.mapper.FusionauthMapper;
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
 * Handler for authentication request by code
 */
@Component
@RequiredArgsConstructor
@Validated
@Slf4j
public class LoginHandler {

    private final FusionauthPublicApiClient fusionauthPublicApiClient;
    private final FusionauthMapper fusionauthMapper;

    public String handle(@Valid @NotNull(message = "Input parameters are required")
                         Input input) {
        try {
            log.info("[VERBOSE] Received code: '{}'", input.getCode());

            return fusionauthPublicApiClient.authByCode(
                            fusionauthMapper.map(input)
                    ).getBody()
                    .getToken();
        } catch (Exception exception) {
            throw new PasswordlessApiException("Error obtaining token through Passwordless API", true, exception);
        }
    }

    @Builder
    @Value
    public static class Input {
        /**
         * Unique code sent to user by email, required to complete login
         */
        @NotBlank(message = "Unique code sent to user by email must not be empty")
        String code;
        /**
         * TODO Development question: we may want to validate the secret in the code request and login response
         */
        String stateSecret;
    }
}
