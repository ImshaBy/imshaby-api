package by.imsha.domain.dto;

import by.imsha.domain.Mass;
import by.imsha.domain.dto.mapper.MassInfoMapper;
import by.imsha.serializers.LocalDateSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alena Misan
 */
public class MassSchedule implements Serializable {

    @Getter
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate startWeekDate;

    @Getter
    @Setter
    private MassNav nav;

    /**
     * Флаг установко которого гарантирует валидность месс (по датам начала и конца)
     * <p>
     * введен для добавление отдельных ветвей логики, при переходе на meilisearch
     */
    @JsonIgnore
    private final boolean onlyValidMasses;

    @Getter
    private List<MassDay> schedule;

    private MassInfoMapper massInfoMapper;

    public MassSchedule(LocalDate startDate, MassInfoMapper massInfoMapper) {
        this(startDate, massInfoMapper, false);
    }

    public MassSchedule(LocalDate startDate, MassInfoMapper massInfoMapper, boolean onlyValidMasses) {
        this.startWeekDate = startDate;
        weekMasses = new HashMap<>();
        massesByDay = new HashMap<>();
        schedule = new ArrayList<>();
        this.onlyValidMasses = onlyValidMasses;
        this.massInfoMapper = massInfoMapper;
    }

    @Getter
    @JsonIgnore
    @Deprecated(forRemoval = true)
    private Map<WeekDayTimeKey, List<Mass>> weekMasses;

    @JsonIgnore
    private final Map<DayOfWeek, Map<LocalTime, List<MassInfo>>> massesByDay;

    private final Map<LocalDate, Map<LocalTime, List<MassInfo>>> occurrencesByDate = new HashMap<>();

    @JsonIgnore
    public Map<DayOfWeek, Map<LocalTime, List<MassInfo>>> getMassesByDay() {
        if (occurrencesByDate.isEmpty()) {
            return massesByDay;
        }
        Map<DayOfWeek, Map<LocalTime, List<MassInfo>>> combined = new HashMap<>();
        massesByDay.forEach((day, hours) -> mergeHours(combined.computeIfAbsent(day, key -> new HashMap<>()), hours));
        occurrencesByDate.forEach((date, hours) ->
                mergeHours(combined.computeIfAbsent(date.getDayOfWeek(), key -> new HashMap<>()), hours));
        return combined;
    }

    public void populateOccurrence(MassInfo mass, LocalDateTime occurrence) {
        occurrencesByDate.computeIfAbsent(occurrence.toLocalDate(), date -> new HashMap<>())
                .computeIfAbsent(occurrence.toLocalTime(), time -> new ArrayList<>())
                .add(mass);
    }

    private static void mergeHours(Map<LocalTime, List<MassInfo>> target, Map<LocalTime, List<MassInfo>> source) {
        source.forEach((time, masses) -> target.computeIfAbsent(time, key -> new ArrayList<>()).addAll(masses));
    }

    public void populateContainers(Mass mass, DayOfWeek dayOfWeek, LocalTime time) {
        populateContainers(massInfoMapper.toMassInfo(mass), dayOfWeek, time);
    }

    public void populateContainers(final MassInfo massInfo, final DayOfWeek dayOfWeek, final LocalTime time) {
        massesByDay.computeIfAbsent(dayOfWeek, v -> new HashMap<>())
                .computeIfAbsent(time, v -> new ArrayList<>())
                .add(massInfo);
    }


    /**
     * Build schedule for week from provided startWeekDate;
     */
    public MassSchedule createSchedule() {
//        LocalDate startDate = LocalDate.of(getStartWeekDate().getYear(), getStartWeekDate().getMonth(), getStartWeekDate().getDayOfMonth()).minusDays(getStartWeekDate().getDayOfWeek().getValue() - 1)
//                .minusDays(getStartWeekDate().getDayOfWeek().getValue() - 1);

        createSchedule(getStartWeekDate(), 7);
        return this;
    }

    /**
     * Build schedule for week from provided startWeekDate;
     */
    public MassSchedule createSchedule(int days) {
        createSchedule(getStartWeekDate(), days);
        return this;
    }

    private void createSchedule(LocalDate startDate, int days) {
        int counter = 0;

        while (counter < days) {
            final LocalDate date = startDate;
            Map<LocalTime, List<MassInfo>> legacyHours = massesByDay.getOrDefault(date.getDayOfWeek(), Map.of());
            if (!onlyValidMasses) {
                legacyHours.values().forEach(masses -> masses.removeIf(mass ->
                        (mass.getStartDate() != null && mass.getStartDate().isAfter(date))
                                || (mass.getEndDate() != null && mass.getEndDate().isBefore(date))));
            }
            Map<LocalTime, List<MassInfo>> hours = new HashMap<>();
            mergeHours(hours, legacyHours);
            mergeHours(hours, occurrencesByDate.getOrDefault(date, Map.of()));
            List<MassDay.MassHour> massHours = hours.entrySet().stream()
                    .filter(entry -> !entry.getValue().isEmpty())
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new MassDay.MassHour(entry.getKey(), entry.getValue()))
                    .toList();
            if (!massHours.isEmpty()) {
                MassDay massDay = new MassDay(date);
                massDay.setMassHours(massHours);
                schedule.add(massDay);
            }

            startDate = startDate.plusDays(1);
            counter++;
        }
    }


    @Deprecated(forRemoval = true)
    private class WeekDayTimeKey {
        private DayOfWeek weekDay;
        private LocalTime time;

        WeekDayTimeKey(DayOfWeek weekDay, LocalTime time) {
            this.weekDay = weekDay;
            this.time = time;
        }

        DayOfWeek getWeekDay() {
            return weekDay;
        }

        void setWeekDay(DayOfWeek weekDay) {
            this.weekDay = weekDay;
        }

        LocalTime getTime() {
            return time;
        }

        void setTime(LocalTime time) {
            this.time = time;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WeekDayTimeKey)) return false;

            WeekDayTimeKey that = (WeekDayTimeKey) o;

            if (weekDay != that.weekDay) return false;
            if (!time.equals(that.time)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = weekDay.getValue();
            result = 31 * result + time.hashCode();
            return result;
        }

    }

}
