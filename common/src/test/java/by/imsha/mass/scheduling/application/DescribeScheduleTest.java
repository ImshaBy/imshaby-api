package by.imsha.mass.scheduling.application;

import by.imsha.mass.scheduling.domain.CalendarSchedule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DescribeScheduleTest {
    private static final ZoneId MINSK = ZoneId.of("Europe/Minsk");

    @Test
    void weeklyStartsOnFirstSelectedDayAndIncludesTheEndDate() {
        int[] weekdays = {7, 1, 5};
        CalendarSchedule schedule = DescribeSchedule.weekly(LocalDate.of(2025, 1, 4),
                LocalDate.of(2025, 1, 10), LocalTime.of(1, 30), weekdays, MINSK);

        assertThat(schedule.dtstart()).isEqualTo("DTSTART;TZID=Europe/Minsk:20250105T013000");
        assertThat(schedule.rrule()).isEqualTo("FREQ=WEEKLY;BYDAY=MO,FR,SU;UNTIL=20250109T223000Z");
        assertThat(weekdays).containsExactly(7, 1, 5);
    }

    @Test
    void endOnlySeriesHasAnAnchorBeforeItsInclusiveEnd() {
        CalendarSchedule schedule = DescribeSchedule.weekly(null, LocalDate.of(1960, 1, 3),
                LocalTime.of(8, 0), new int[]{7}, MINSK);

        assertThat(schedule.dtstart()).isEqualTo("DTSTART;TZID=Europe/Minsk:00010107T080000");
        assertThat(schedule.rrule()).isEqualTo("FREQ=WEEKLY;BYDAY=SU;UNTIL=19600103T050000Z");
    }
}
