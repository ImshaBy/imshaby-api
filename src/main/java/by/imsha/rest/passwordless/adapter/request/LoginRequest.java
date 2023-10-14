package by.imsha.rest.passwordless.adapter.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Данные для запроса на аутентификацию по коду
 */
@Data
public class LoginRequest {

    @NotBlank(message = "PASSWORDLESS.002")
    private String code;
}
