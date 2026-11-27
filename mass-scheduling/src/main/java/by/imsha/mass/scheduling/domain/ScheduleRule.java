package by.imsha.mass.scheduling.domain;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.transform.recurrence.Frequency;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ScheduleRule {
    private final ZonedDateTime start;
    private final Recur<Temporal> recurrence;
    private final Instant until;
    private final Set<Instant> exclusions;

    private ScheduleRule(ZonedDateTime start, Recur<Temporal> recurrence, Instant until,
                         Set<Instant> exclusions) {
        this.start = start;
        this.recurrence = recurrence;
        this.until = until;
        this.exclusions = exclusions;
    }

    public static ScheduleRule parse(CalendarSchedule calendar) {
        ZonedDateTime start = calendar.dtstart().atZone(ZoneOffset.UTC);

        var recurrence = new Recur<Temporal>(calendar.rrule(), false);
        Instant until = recurrence.getUntil() == null ? null : Instant.from(recurrence.getUntil());
        return new ScheduleRule(start, recurrence, until, Set.copyOf(calendar.exdates()));
    }

    public ZonedDateTime start() {
        return start;
    }

    public Instant until() {
        return until;
    }

    public Set<Instant> exclusions() {
        return exclusions;
    }

    public Set<DayOfWeek> weekdays() {
        return recurrence.getDayList().stream().map(WeekDay::getDayOfWeek).collect(Collectors.toUnmodifiableSet());
    }

    public boolean monthly() {
        return recurrence.getFrequency() == Frequency.MONTHLY;
    }

    public boolean single() {
        return recurrence.getFrequency() == Frequency.DAILY && start.toInstant().equals(until);
    }

    public String byDay() {
        return recurrence.getDayList().stream().distinct().sorted(Comparator.comparing(WeekDay::getDayOfWeek))
                .map(WeekDay::toString).collect(Collectors.joining(","));
    }

    public boolean contains(Instant instant) {
        // Membership includes cancelled dates so repeated cancellation is idempotent.
        return starts(instant, instant.plusSeconds(1)).anyMatch(instant::equals);
    }

    public Stream<LocalDateTime> occurrences(LocalDate from, LocalDate toExclusive, ZoneId outputZone) {
        return occurrences(from.atStartOfDay(outputZone).toInstant(), toExclusive.atStartOfDay(outputZone).toInstant())
                .map(instant -> LocalDateTime.ofInstant(instant, outputZone));
    }

    public Stream<Instant> occurrences(Instant from, Instant toExclusive) {
        return starts(from, toExclusive).filter(instant -> !exclusions.contains(instant));
    }

    /**
     * Returns RRULE starts in [lowerBound, upperBound), before applying EXDATE.
     */
    public Stream<Instant> starts(Instant lowerBound, Instant upperBound) {
        if (!lowerBound.isBefore(upperBound) || !start.toInstant().isBefore(upperBound)
                || until != null && (until.isBefore(lowerBound) || until.isBefore(start.toInstant()))) {
            return Stream.empty();
        }
        ZoneId zone = start.getZone();
        Instant last = until != null && until.isBefore(upperBound) ? until : upperBound;
        LocalDateTime from = LocalDate.ofInstant(lowerBound, zone).minusDays(1).atStartOfDay();
        LocalDateTime through = LocalDate.ofInstant(last, zone).plusDays(1).atStartOfDay();
        return recurrence.getDates(start, from.atZone(zone), through.atZone(zone)).stream()
                .map(Instant::from)
                .filter(instant -> !instant.isBefore(lowerBound) && instant.isBefore(upperBound)
                        && !instant.isBefore(start.toInstant()) && (until == null || !instant.isAfter(until)));
    }
}
