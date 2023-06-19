package by.imsha.properties;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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

    @Data
    public static class City {
        /**
         * Наименование города
         */
        @NotBlank
        public String name;
    }
}
