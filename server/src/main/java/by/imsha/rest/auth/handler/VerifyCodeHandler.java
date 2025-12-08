package by.imsha.rest.auth.handler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Validated
@Slf4j
public class VerifyCodeHandler {

    private final ConfirmationCodeGenerator confirmationCodeGenerator;

    public boolean handle(@Valid @NotNull(message = "Input parameters are required")
                          Input input) {

        Optional<String> optionalConfirmationCode = confirmationCodeGenerator.getCode(input.getEmail());

        if (optionalConfirmationCode.isEmpty()) {
            log.error("Error verifying code. Code for corresponding email is not present in cache. {}", input);
            return false;
        }

        String confirmationCode = optionalConfirmationCode.get();

        if (confirmationCode.equals(input.getConfirmationCode())) {
            log.info("Successfully verified code. {}", input);
            return true;
        } else {
            log.error("Invalid confirmation code received. Expected code: {}. Received data: {}", confirmationCode,
                    input);
            return false;
        }
    }

    @Builder
    @Value
    public static class Input {

        /**
         * email пользователя
         */
        @NotBlank(message = "User email must be filled")
        String email;

        /**
         * Код подтверждения
         */
        @NotBlank(message = "Confirmation code must be filled")
        String confirmationCode;
    }
}
