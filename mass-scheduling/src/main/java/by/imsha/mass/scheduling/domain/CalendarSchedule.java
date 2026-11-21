package by.imsha.mass.scheduling.domain;

import java.util.Objects;

public record CalendarSchedule(String dtstart, String rrule) {
    public CalendarSchedule {
        Objects.requireNonNull(dtstart);
        Objects.requireNonNull(rrule);
    }
}
