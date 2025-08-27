package by.imsha.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * Параметры для работы с fusionAuth
 */
@Data
@Validated
public class FusionauthProperties {

    @NotNull
    private Integer userSearchPagination;

    /**
     * url сервера FusionAuth
     */
    @NotEmpty
    private String url;

    /**
     * Пока альтернатив нет
     * Скорее всего в будущем будет несколько приложений и нужно будет перейти на какую-то коллекцию
     */
    @NotBlank
    @Deprecated
    private String applicationId;

    /**
     *  Идентификатор шаблона сообщения с кодом подтверждения
     */
    private String confirmationCodeEmailTemplateId;

    @NotNull
    private Security security;

    @Data
    public static class Security {

        @NotNull
        private ApiKey apiKey;
    }

    @Data
    public static class ApiKey {

        /**
         * api-key для доступа к FusionAuth
         */
        @NotEmpty
        private String key;
    }
}
