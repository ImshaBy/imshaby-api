package by.imsha.mass.scheduling.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public record CalendarSchedule(Instant dtstart, String rrule, List<Instant> exdates) {
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final Pattern UNTIL = Pattern.compile("(?i)(?:^UNTIL=[^;]+;?|;UNTIL=[^;]+)");
    private static final Pattern COUNT = Pattern.compile("(?i)(?:^|;)COUNT=");

    public CalendarSchedule {
        Objects.requireNonNull(dtstart);
        Objects.requireNonNull(rrule);
        exdates = exdates == null ? List.of() : List.copyOf(exdates);
    }

    public CalendarSchedule(Instant dtstart, String rrule) {
        this(dtstart, rrule, List.of());
    }

    public static CalendarSchedule once(Instant start) {
        return new CalendarSchedule(start, "FREQ=DAILY").withUntil(start);
    }

    public CalendarSchedule withUntil(Instant end) {
        if (COUNT.matcher(rrule).find()) {
            throw new IllegalArgumentException("RRULE cannot combine COUNT and UNTIL");
        }
        return new CalendarSchedule(dtstart, UNTIL.matcher(rrule).replaceAll("") + ";UNTIL=" + UTC.format(end),
                exdates);
    }
}
