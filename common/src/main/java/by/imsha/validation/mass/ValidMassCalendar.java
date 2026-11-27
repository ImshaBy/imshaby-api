package by.imsha.validation.mass;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Constraint(validatedBy = MassCalendarValidator.class)
public @interface ValidMassCalendar {
    String message();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
