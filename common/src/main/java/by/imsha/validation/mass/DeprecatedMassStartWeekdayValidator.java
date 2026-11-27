package by.imsha.validation.mass;

import by.imsha.domain.DeprecatedMass;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class DeprecatedMassStartWeekdayValidator implements ConstraintValidator<DeprecatedMassStartWeekdayValid, DeprecatedMass> {

    @Override
    public boolean isValid(DeprecatedMass mass, ConstraintValidatorContext context) {
        if (mass == null || !mass.isPeriodic() || mass.getStartDate() == null || mass.getDays() == null) {
            return true;
        }

        int weekday = mass.getStartDate().getDayOfWeek().getValue();
        if (Arrays.stream(mass.getDays()).anyMatch(day -> day == weekday)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("startDate")
                .addConstraintViolation();
        return false;
    }
}
