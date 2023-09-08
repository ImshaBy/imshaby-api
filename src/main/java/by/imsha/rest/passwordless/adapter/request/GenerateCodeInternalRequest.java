package by.imsha.rest.passwordless.adapter.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Данные запроса, для получения кода для беспарольной аутентификации
 */
@Data
public class GenerateCodeInternalRequest {

    @NotBlank(message = "email не должен быть пустым")
    String email;
}