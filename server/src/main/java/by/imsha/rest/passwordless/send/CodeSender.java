package by.imsha.rest.passwordless.send;

import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public interface CodeSender {

    void send(final String userIdentifier,
                     @NotBlank(message = "Уникальный код не должен быть пустым") String code);

}
