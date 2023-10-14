package by.imsha.validation.common;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Проверить, что локаль входит в список доступных
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Constraint(validatedBy = AvailableLocaleValidator.class)
@Repeatable(AvailableLocale.List.class)
public @interface AvailableLocale {

    /**
     * Наименование поля, с которым будет связана ошибка
     */
    String field();

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        AvailableLocale[] value();
    }
}
