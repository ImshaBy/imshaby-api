package by.imsha.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class AuthProperties {

    /**
     * Префикс свойств
     */
    public static final String PREFIX = "app.auth";

    /**
     *  Идентификатор шаблона сообщения с кодом подтверждения
     */
    @NotBlank
    private String confirmationCodeEmailTemplateId;

    /**
     * URI ресурсов api
     */
    @NotNull
    private AuthProperties.UriContainer uri;

    @Data
    public static class UriContainer {
        /**
         * URI для отправки сообщения пользователю на email
         */
        @NotBlank
        private String sendEmail;
    }

}
