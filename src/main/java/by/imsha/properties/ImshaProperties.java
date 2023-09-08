package by.imsha.properties;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * Параметры приложения imshaby
 */
@Data
@Validated
public class ImshaProperties {

    /**
     * Токен аутентификации (куда-то?)
     */
    @NotBlank
    private String webHookToken;
    /**
     * Имя куки локали
     */
    @NotBlank
    private String langCookie;
    /**
     * Параметры города по-умолчанию
     */
    @NotNull
    private City defaultCity;
    /**
     * Набор API-ключей
     */
    Set<String> apiKeys;
    /**
     * Набор API-ключей для внутреннего использования
     */
    Set<String> internalApiKeys;

    @Data
    public static class City {
        /**
         * Наименование города
         */
        @NotBlank
        public String name;
    }
}
