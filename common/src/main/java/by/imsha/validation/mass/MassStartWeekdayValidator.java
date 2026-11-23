package by.imsha.validation.mass;

import by.imsha.domain.Mass;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class MassStartWeekdayValidator implements ConstraintValidator<MassStartWeekdayValid, Mass> {

    @Override
    public boolean isValid(Mass mass, ConstraintValidatorContext context) {
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
