package by.imsha.validation.mass;

import by.imsha.domain.Mass.CalendarDescription;
import by.imsha.mass.scheduling.domain.CalendarSchedule;
import by.imsha.mass.scheduling.domain.ScheduleRule;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.DateTimeException;

public class MassCalendarValidator implements ConstraintValidator<ValidMassCalendar, CalendarDescription> {
    @Override
    public boolean isValid(CalendarDescription calendar, ConstraintValidatorContext context) {
        if (calendar == null) {
            return true; // @NotNull reports a missing calendar.
        }
        var start = calendar.dtstart();
        if (start == null || start.getNano() != 0 || calendar.rrule() == null || calendar.rrule().isBlank()) {
            return false;
        }
        try {
            var rule = ScheduleRule.parse(new CalendarSchedule(start, calendar.rrule(), calendar.exdates()));
            return rule.contains(start);
        } catch (IllegalArgumentException | DateTimeException invalidCalendar) {
            return false;
        }
    }
}
