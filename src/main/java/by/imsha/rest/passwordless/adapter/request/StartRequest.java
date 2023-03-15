package by.imsha.rest.passwordless.adapter.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Данные запроса, для запуска процесса беспарольной аутентификации
 */
@Data
public class StartRequest {

    @NotBlank(message = "email не должен быть пустым")
    String email;
}
