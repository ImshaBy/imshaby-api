package by.imsha.server.rest.passwordless.adapter.request;

import by.imsha.serializers.TrimStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Данные запроса, для запуска процесса беспарольной аутентификации
 */
@Data
public class StartRequest {

    @JsonDeserialize(using = TrimStringDeserializer.class)
    @NotBlank(message = "PASSWORDLESS.001")
    String email;
}
