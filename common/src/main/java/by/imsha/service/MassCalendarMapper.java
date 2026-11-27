package by.imsha.service;

import by.imsha.domain.Mass.CalendarDescription;
import by.imsha.mass.scheduling.domain.CalendarSchedule;
import by.imsha.mass.scheduling.domain.ScheduleRule;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MassCalendarMapper {
    default ScheduleRule toScheduleRule(CalendarDescription calendar) {
        return ScheduleRule.parse(new CalendarSchedule(calendar.dtstart(), calendar.rrule(), calendar.exdates()));
    }
}
