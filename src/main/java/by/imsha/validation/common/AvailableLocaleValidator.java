package by.imsha.validation.common;

import org.apache.commons.lang3.LocaleUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Locale;

/**
 * Валидатор, проверяющий, что локаль входит в список доступных
 */
public class AvailableLocaleValidator implements ConstraintValidator<AvailableLocale, Locale> {

    private AvailableLocale constraintAnnotation;

    @Override
    public void initialize(AvailableLocale constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(Locale value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (!LocaleUtils.isAvailableLocale(value)) {
            //если поля не прошли проверку, прикрепляем ошибку к первому из них
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(constraintAnnotation.field())
                    .addConstraintViolation();
            context.disableDefaultConstraintViolation();
            return false;
        }

        return true;
    }
}
