package by.imsha.service;

import by.imsha.domain.Mass;
import by.imsha.mass.scheduling.domain.CalendarSchedule;
import by.imsha.mass.scheduling.domain.ScheduleRule;

import java.util.ArrayList;
import java.util.TreeSet;

import static by.imsha.utils.ServiceUtils.BEL_ZONE_ID;

final class MassCalendar {
    private MassCalendar() {
    }

    static ScheduleRule ruleOf(Mass mass) {
        var calendar = mass.getCalendar();
        return ScheduleRule.parse(new CalendarSchedule(calendar.dtstart(), calendar.rrule()));
    }

    static Mass readView(Mass mass) {
        return mass.getCalendar() == null ? mass : readView(mass, ruleOf(mass));
    }

    static Mass readView(Mass mass, ScheduleRule rule) {
        Mass view = new Mass(mass);
        view.setId(mass.getId());
        view.setLastModifiedDate(mass.getLastModifiedDate());
        if (rule.single()) {
            view.setSingleStartTimestamp(rule.start().toEpochSecond());
            view.setTime(null);
            view.setDays(mass.getDays() != null && mass.getDays().length == 0 ? new int[0] : null);
        } else {
            view.setSingleStartTimestamp(0);
            var localStart = rule.start().withZoneSameInstant(BEL_ZONE_ID);
            view.setTime(localStart.toLocalTime().toString());
            int dayShift = localStart.getDayOfWeek().getValue() - rule.start().getDayOfWeek().getValue();
            var remaining = new TreeSet<Integer>();
            rule.weekdays().forEach(day -> remaining.add(day.plus(dayShift).getValue()));
            var orderedDays = new ArrayList<Integer>();
            if (mass.getDays() != null) {
                for (int day : mass.getDays()) {
                    if (remaining.remove(day)) {
                        orderedDays.add(day);
                    }
                }
            }
            orderedDays.addAll(remaining);
            view.setDays(orderedDays.stream().mapToInt(Integer::intValue).toArray());
        }
        return view;
    }
}
