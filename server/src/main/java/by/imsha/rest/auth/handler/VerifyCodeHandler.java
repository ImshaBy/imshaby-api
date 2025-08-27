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

    public boolean handle(@Valid @NotNull(message = "Входные параметры обязательны для заполнения")
                          Input input) {

        Optional<String> optionalConfirmationCode = confirmationCodeGenerator.getCode(input.getEmail());

        if (optionalConfirmationCode.isEmpty()) {
            log.error("Ошибка при проверке кода. Код для соответствующего email отсутствует в кэше. {}", input);
            return false;
        }

        String confirmationCode = optionalConfirmationCode.get();

        if (confirmationCode.equals(input.getConfirmationCode())) {
            log.info("Успешно пройдена проверка кода. {}", input);
            return true;
        } else {
            log.error("Получен неверный код подтверждения. Ожидаемый код: {}. Полученные данные: {}", confirmationCode,
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
        @NotBlank(message = "Адрес электронной почты пользователя должен быть заполнен")
        String email;

        /**
         * Код подтверждения
         */
        @NotBlank(message = "Код подтверждения должен быть заполнен")
        String confirmationCode;
    }
}
