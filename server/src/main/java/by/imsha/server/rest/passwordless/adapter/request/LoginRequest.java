package by.imsha.server.rest.passwordless.adapter.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Данные для запроса на аутентификацию по коду
 */
@Data
public class LoginRequest {

    @NotBlank(message = "PASSWORDLESS.002")
    private String code;
}
