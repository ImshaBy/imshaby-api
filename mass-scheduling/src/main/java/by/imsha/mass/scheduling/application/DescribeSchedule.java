package by.imsha.mass.scheduling.application;

import by.imsha.mass.scheduling.domain.CalendarSchedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class DescribeSchedule {
    // RFC 5545 uses four-digit years. An open lower bound needs a stable calendar anchor.
    private static final LocalDate EARLIEST_DATE = LocalDate.of(1, 1, 1);
    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss");
    private static final DateTimeFormatter UTC = LOCAL.withZone(ZoneOffset.UTC);
    private static final String[] WEEKDAYS = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};

    private DescribeSchedule() {
    }

    public static CalendarSchedule once(Instant start) {
        String utcStart = formatUtc(start);
        return new CalendarSchedule("DTSTART:" + utcStart, "FREQ=DAILY;UNTIL=" + utcStart);
    }

    public static CalendarSchedule weekly(LocalDate from, LocalDate through, LocalTime time,
                                          int[] weekdays, ZoneId zone) {
        LocalDate lowerBound = from == null ? EARLIEST_DATE : from;
        int firstDay = lowerBound.getDayOfWeek().getValue();
        int daysUntilFirst = Arrays.stream(weekdays)
                .map(day -> Math.floorMod(day - firstDay, DayOfWeek.SUNDAY.getValue()))
                .min()
                .orElseThrow();
        LocalDate first = lowerBound.plusDays(daysUntilFirst);
        String byDay = Arrays.stream(weekdays).sorted()
                .mapToObj(day -> WEEKDAYS[day - 1])
                .collect(Collectors.joining(","));
        String rule = "FREQ=WEEKLY;BYDAY=" + byDay;
        if (through != null) {
            rule += ";UNTIL=" + formatUtc(through.atTime(time).atZone(zone).toInstant());
        }
        return new CalendarSchedule("DTSTART;TZID=" + zone.getId() + ":" + LOCAL.format(first.atTime(time)), rule);
    }

    private static String formatUtc(Instant instant) {
        return UTC.format(instant) + "Z";
    }
}
