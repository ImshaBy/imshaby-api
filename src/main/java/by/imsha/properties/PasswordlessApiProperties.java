package by.imsha.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Validated
public class PasswordlessApiProperties {

    /**
     * API ключ, позволяющий выполнять запросы к passwordless api
     */
    @NotBlank
    private String apiKey;

    /**
     * Пока альтернатив нет
     * Скорее всего в будущем будет несколько приложений и нужно будет перейти на какую-то коллекцию
     */
    @NotBlank
    @Deprecated
    private String applicationId;

    /**
     * URI ресурсов passwordless api
     */
    @NotNull
    private PasswordlessUriContainer uri;

    @Data
    public static class PasswordlessUriContainer {
        /**
         * URI для инициализации процесса беспарольного входа
         */
        @NotBlank
        private String start;
        /**
         * URI для отправки уведомления на почту пользователю
         */
        @NotBlank
        private String send;
        /**
         * URI для завершения процесса беспарольного входа
         */
        @NotBlank
        private String login;
    }

}
