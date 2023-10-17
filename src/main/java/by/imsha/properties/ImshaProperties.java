package by.imsha.properties;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Параметры приложения imshaby
 */
@Data
@Validated
public class ImshaProperties {

    /**
     * Идентификатор временной зоны
     */
    @NotNull
    private ZoneId zoneId;
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
    /**
     * Набор API-ключей для запроса расписания месс по парафии
     * (ключ - API-ключ, значение - соответствующий ключ парафии)
     */
    @NotNull
    ParishWeekApiKeys parishWeekApiKeys;

    @Data
    public static class City {
        /**
         * Key города
         */
        @NotBlank
        public String key;
    }

    /**
     * Обёртка для конфигурации API-ключей для получения расписаний по парафии
     */
    @Data
    public static class ParishWeekApiKeys {
        /**
         * Ключ - API-ключ, значение - ключ парафии
         */
        private Map<String, String> map = new HashMap<>();
    }
}
