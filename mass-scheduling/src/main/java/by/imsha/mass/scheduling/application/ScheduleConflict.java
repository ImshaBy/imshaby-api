package by.imsha.mass.scheduling.application;

import by.imsha.mass.scheduling.domain.ScheduleRule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

public final class ScheduleConflict {
    private ScheduleConflict() {
    }

    public static boolean intersects(ScheduleRule first, ScheduleRule second) {
        Instant lower = first.start().isAfter(second.start()) ? first.start().toInstant() : second.start().toInstant();
        Instant end = first.until();
        if (end == null || second.until() != null && second.until().isBefore(end)) {
            end = second.until();
        }
        Instant upper;
        if (end == null) {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            Instant startOfToday = today.atStartOfDay().toInstant(ZoneOffset.UTC);
            if (lower.isBefore(startOfToday)) {
                lower = startOfToday;
            }
            upper = today.plusYears(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        } else {
            upper = end.plusNanos(1);
        }
        var starts = first.occurrences(lower, upper).collect(Collectors.toSet());
        return !starts.isEmpty() && second.occurrences(lower, upper).anyMatch(starts::contains);
    }
}
