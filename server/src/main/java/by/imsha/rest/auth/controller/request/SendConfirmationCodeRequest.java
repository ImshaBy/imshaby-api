package by.imsha.rest.auth.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Данные запроса для отправки кода подтверждения
 */
@Data
public class SendConfirmationCodeRequest {


    @NotBlank(message = "email не должен быть пустым")
    String email;
}
