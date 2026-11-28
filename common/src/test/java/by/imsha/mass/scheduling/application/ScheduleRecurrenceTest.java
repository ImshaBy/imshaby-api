package by.imsha.mass.scheduling.application;

import by.imsha.mass.scheduling.domain.CalendarSchedule;
import by.imsha.mass.scheduling.domain.ScheduleRule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleRecurrenceTest {
    private static final ZoneId MINSK = ZoneId.of("Europe/Minsk");

    @Test
    void expandsOrdinalWeekdays() {
        assertDates("1MO", "2026-01-05", "2026-01-05", "2026-02-02", "2026-03-02");
        assertDates("-1SU", "2026-01-25", "2026-01-25", "2026-02-22", "2026-03-29");
        assertDates("5FR", "2026-01-30", "2026-01-30");
    }

    @Test
    void includesUntilAndExcludesACancelledDate() {
        var rule = ScheduleRule.parse(new CalendarSchedule(Instant.parse("2026-01-05T05:00:00Z"),
                "FREQ=MONTHLY;BYDAY=1MO;UNTIL=20260302T050000Z",
                List.of(Instant.parse("2026-02-02T05:00:00Z"))));

        assertThat(rule.contains(Instant.parse("2026-02-02T05:00:00Z"))).isTrue();
        assertThat(rule.contains(Instant.parse("2026-03-02T05:00:00Z"))).isTrue();
        assertThat(rule.contains(Instant.parse("2026-04-06T05:00:00Z"))).isFalse();
        assertThat(rule.occurrences(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-05-01"), MINSK))
                .containsExactly(LocalDateTime.parse("2026-01-05T08:00"), LocalDateTime.parse("2026-03-02T08:00"));
    }

    @Test
    void retainsHistoricalOccurrencesFromAStoredAnchor() {
        var rule = ScheduleRule.parse(new CalendarSchedule(Instant.parse("1959-12-21T05:00:00Z"),
                "FREQ=WEEKLY;BYDAY=MO"));
        assertThat(rule.occurrences(LocalDate.parse("1960-01-01"), LocalDate.parse("1960-01-11"), MINSK))
                .containsExactly(LocalDateTime.parse("1960-01-04T08:00"));
    }

    private static void assertDates(String byDay, String start, String... expected) {
        var schedule = new CalendarSchedule(Instant.parse(start + "T05:00:00Z"),
                "FREQ=MONTHLY;BYDAY=" + byDay);
        assertThat(ScheduleRule.parse(schedule)
                .occurrences(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-04-01"), MINSK)
                .map(LocalDateTime::toLocalDate).map(LocalDate::toString)).containsExactly(expected);
    }
}
