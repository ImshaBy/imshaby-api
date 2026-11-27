package by.imsha.domain.dto.mapper;

import by.imsha.domain.DeprecatedMass;
import by.imsha.domain.Mass;
import by.imsha.mass.scheduling.domain.CalendarSchedule;
import by.imsha.mass.scheduling.domain.ScheduleRule;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static by.imsha.utils.ServiceUtils.BEL_ZONE_ID;

@Mapper(componentModel = "spring")
public abstract class DeprecatedMassMapper {
    private static final List<String> WEEKDAYS = List.of("MO", "TU", "WE", "TH", "FR", "SA", "SU");

    @Mapping(target = "calendar", source = "mass")
    public abstract Mass toMass(DeprecatedMass mass);

    @Mapping(target = "singleStartTimestamp", ignore = true)
    @Mapping(target = "time", ignore = true)
    @Mapping(target = "days", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", ignore = true)
    public abstract DeprecatedMass toDeprecatedMass(Mass mass);

    protected Mass.CalendarDescription toCalendar(DeprecatedMass mass) {
        if (!mass.isPeriodic()) {
            var calendar = CalendarSchedule.once(Instant.ofEpochSecond(mass.getSingleStartTimestamp()));
            return new Mass.CalendarDescription(calendar.dtstart(), calendar.rrule());
        }
        var time = LocalTime.parse(mass.getTime());
        var localStart = mass.getStartDate().atTime(time).atZone(BEL_ZONE_ID);
        var utcStart = localStart.withZoneSameInstant(ZoneOffset.UTC);
        long dayShift = utcStart.toLocalDate().toEpochDay() - localStart.toLocalDate().toEpochDay();
        String days = Arrays.stream(mass.getDays())
                .mapToObj(day -> WEEKDAYS.get(DayOfWeek.of(day).plus(dayShift).getValue() - 1))
                .collect(Collectors.joining(","));
        var calendar = new CalendarSchedule(utcStart.toInstant(), "FREQ=WEEKLY;BYDAY=" + days);
        if (mass.getEndDate() != null) {
            var end = mass.getEndDate().atTime(time).atZone(BEL_ZONE_ID);
            calendar = calendar.withUntil(end.toInstant());
        }
        return new Mass.CalendarDescription(calendar.dtstart(), calendar.rrule());
    }

    @AfterMapping
    protected void readCalendar(Mass mass, @MappingTarget DeprecatedMass target) {
        var calendar = mass.getCalendar();
        var rule = ScheduleRule.parse(new CalendarSchedule(calendar.dtstart(), calendar.rrule(), calendar.exdates()));
        if (rule.single()) {
            target.setSingleStartTimestamp(rule.start().toEpochSecond());
            return;
        }
        if (rule.monthly()) {
            throw new IllegalArgumentException("Monthly schedules cannot be represented in v1");
        }
        var start = rule.start().withZoneSameInstant(BEL_ZONE_ID);
        target.setStartDate(start.toLocalDate());
        target.setTime(start.toLocalTime().toString());
        target.setEndDate(rule.until() == null ? null : LocalDate.ofInstant(rule.until(), BEL_ZONE_ID));
        String byDay = Arrays.stream(calendar.rrule().split(";"))
                .filter(part -> part.startsWith("BYDAY=")).findFirst().orElseThrow();
        int shift = start.getDayOfWeek().getValue() - rule.start().getDayOfWeek().getValue();
        target.setDays(Arrays.stream(byDay.substring("BYDAY=".length()).split(","))
                .mapToInt(day -> Math.floorMod(WEEKDAYS.indexOf(day) + shift, 7) + 1).toArray());
    }
}
