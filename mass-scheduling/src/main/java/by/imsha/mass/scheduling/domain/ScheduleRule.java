package by.imsha.mass.scheduling.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ScheduleRule(ZonedDateTime start, Set<DayOfWeek> weekdays, Instant until) {
    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss");
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'");
    private static final List<String> WEEKDAYS = List.of("MO", "TU", "WE", "TH", "FR", "SA", "SU");

    public ScheduleRule {
        weekdays = Set.copyOf(weekdays);
    }

    public static ScheduleRule parse(CalendarSchedule calendar) {
        String[] startParts = calendar.dtstart().split(":", 2);
        ZonedDateTime start = startParts[0].equals("DTSTART")
                ? parseUtc(startParts[1]).atZone(ZoneOffset.UTC)
                : LocalDateTime.parse(startParts[1], LOCAL)
                        .atZone(ZoneId.of(startParts[0].substring("DTSTART;TZID=".length())));
        var parts = Arrays.stream(calendar.rrule().split(";"))
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(part -> part[0], part -> part[1]));
        Instant until = parts.containsKey("UNTIL") ? parseUtc(parts.remove("UNTIL")) : null;
        String frequency = parts.remove("FREQ");
        if ("DAILY".equals(frequency) && start.toInstant().equals(until) && parts.isEmpty()) {
            return new ScheduleRule(start, Set.of(), until);
        }
        if ("WEEKLY".equals(frequency) && parts.size() == 1 && parts.containsKey("BYDAY")) {
            Set<DayOfWeek> weekdays = Arrays.stream(parts.get("BYDAY").split(","))
                    .map(day -> DayOfWeek.of(WEEKDAYS.indexOf(day) + 1))
                    .collect(Collectors.toSet());
            return new ScheduleRule(start, weekdays, until);
        }
        throw new IllegalArgumentException("Unsupported mass recurrence: " + calendar.rrule());
    }

    public boolean single() {
        return weekdays.isEmpty();
    }

    public Stream<LocalDateTime> occurrences(LocalDate from, LocalDate toExclusive, ZoneId outputZone) {
        Instant lowerBound = from.atStartOfDay(outputZone).toInstant();
        Instant upperBound = toExclusive.atStartOfDay(outputZone).toInstant();
        Stream<ZonedDateTime> starts;
        if (single()) {
            starts = Stream.of(start);
        } else {
            ZoneId zone = start.getZone();
            LocalDate firstDate = lowerBound.atZone(zone).toLocalDate();
            if (firstDate.isBefore(start.toLocalDate())) {
                firstDate = start.toLocalDate();
            }
            LocalDate lastDate = upperBound.atZone(zone).toLocalDate();
            if (firstDate.isAfter(lastDate)) {
                return Stream.empty();
            }
            starts = firstDate.datesUntil(lastDate.plusDays(1))
                    .filter(date -> weekdays.contains(date.getDayOfWeek()))
                    .map(date -> date.atTime(start.toLocalTime()).atZone(zone));
        }
        return starts.filter(value -> {
            Instant instant = value.toInstant();
            return !instant.isBefore(start.toInstant()) && !instant.isBefore(lowerBound)
                    && instant.isBefore(upperBound) && (until == null || !instant.isAfter(until));
        }).map(value -> value.withZoneSameInstant(outputZone).toLocalDateTime());
    }

    private static Instant parseUtc(String value) {
        return LocalDateTime.parse(value, UTC).toInstant(ZoneOffset.UTC);
    }
}
