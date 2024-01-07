package by.imsha.server.rest.passwordless.adapter.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Данные запроса, для получения кода для беспарольной аутентификации
 */
@Data
public class GenerateCodeInternalRequest {

    @NotBlank(message = "email не должен быть пустым")
    String email;
}