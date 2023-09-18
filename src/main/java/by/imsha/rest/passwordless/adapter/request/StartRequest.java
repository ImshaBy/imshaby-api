package by.imsha.rest.passwordless.adapter.request;

import by.imsha.rest.serializers.TrimStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Данные запроса, для запуска процесса беспарольной аутентификации
 */
@Data
public class StartRequest {

    @JsonDeserialize(using = TrimStringDeserializer.class)
    @NotBlank(message = "email не должен быть пустым")
    String email;
}
