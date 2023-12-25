package by.imsha.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class PasswordlessApiProperties {

    /**
     * Префикс свойств
     */
    public static final String PREFIX = "app.oauth2.passwordless";

    /**
     * Логирование ключа вместо отправки
     */
    @NotNull
    private Boolean logCode;

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
