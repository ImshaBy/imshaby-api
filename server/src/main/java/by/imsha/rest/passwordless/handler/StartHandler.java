package by.imsha.rest.passwordless.handler;

import by.imsha.rest.passwordless.exception.PasswordlessApiException;
import by.imsha.rest.passwordless.mapper.FusionauthMapper;
import by.imsha.rest.passwordless.send.CodeSender;
import api_specification.by.imsha.common.fusionauth.secured_client.api.FusionauthApiClient;
import api_specification.by.imsha.common.fusionauth.secured_client.model.StartPasswordlessLoginRequest;
import api_specification.by.imsha.common.fusionauth.secured_client.model.StartPasswordlessLoginResponse;
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

    public void handle(@Valid @NotNull(message = "Input parameters are required") final Input input) {
        handle(input, defaultCodeSender);
    }

    public void handle(@Valid @NotNull(message = "Input parameters are required") final Input input,
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
            throw new PasswordlessApiException("Error initializing passwordless login process: " + exception.getMessage(),
                    false, exception);
        }
    }

    @Builder
    @Value
    public static class Input {
        /**
         * Unique identifier of the application requesting login
         */
        @NotBlank(message = "Application identifier must not be empty")
        String applicationId;
        /**
         * User identifier for login. Can be either email or username.
         */
        @NotBlank(message = "User identifier (email) must not be empty")
        String loginId;
        /**
         * TODO Development question: we may want to validate the secret in the code request and login response
         */
        String stateSecret;
    }
}
