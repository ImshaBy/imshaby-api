package by.imsha.validation.mass;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = UniqueMassValidator.class)
public @interface UniqueMass {

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
