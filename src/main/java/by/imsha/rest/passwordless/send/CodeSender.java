package by.imsha.rest.passwordless.send;

import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Validated
public interface CodeSender {

    void send(final String userIdentifier,
                     @NotBlank(message = "Уникальный код не должен быть пустым") String code);

}
