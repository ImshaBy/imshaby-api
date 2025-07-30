package by.imsha.rest.auth.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Данные запроса для проверки кода подтверждения
 */
@Data
public class VerifyConfirmationCodeRequest {

    @NotBlank(message = "email не должен быть пустым")
    String email;

    @NotBlank(message = "confirmationCode не должен быть пустым")
    String confirmationCode;
}
