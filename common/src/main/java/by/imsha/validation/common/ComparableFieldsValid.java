package by.imsha.validation.common;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = ComparableFieldsValidator.class)
@Repeatable(ComparableFieldsValid.List.class)
public @interface ComparableFieldsValid {

    /**
     * Наименования сравниваемых полей
     */
    String[] fields();

    /**
     * Условие для сравнения значений полей
     */
    Condition condition();

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Условия для сравнения данных представляющих время
     */
    @Getter
    enum Condition {
        /**
         * ==
         */
        EQUALS,
        /**
         * !=
         */
        NOT_EQUALS,
        /**
         * <
         */
        LESS_THAN,
        /**
         * >=
         */
        GREATER_OR_EQUALS,
        /**
         * >
         */
        GREATER_THAN,
        /**
         * <=
         */
        LESS_OR_EQUALS,
        ;
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        ComparableFieldsValid[] value();
    }
}
