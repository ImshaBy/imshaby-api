package by.imsha.domain.dto;

import by.imsha.domain.Mass;
import by.imsha.domain.dto.mapper.MassInfoMapper;
import by.imsha.serializers.LocalDateSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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

    public MassSchedule(LocalDate startDate) {
        this(startDate, false);
    }

    public MassSchedule(LocalDate startDate, boolean onlyValidMasses) {
        this.startWeekDate = startDate;
        weekMasses = new HashMap<>();
        massesByDay = new HashMap<>();
        schedule = new ArrayList<>();
        this.onlyValidMasses = onlyValidMasses;
    }
    @Getter
    @JsonIgnore
    private Map<WeekDayTimeKey, List<Mass>> weekMasses;

    @Getter
    @JsonIgnore
    private Map<DayOfWeek, Map<LocalTime, List<MassInfo>>> massesByDay;

    public void populateContainers(Mass mass, DayOfWeek dayOfWeek, LocalTime time) {
        addToMassesByDay(mass, dayOfWeek, time);
    }

    public void populateContainers(final MassInfo massInfo, final DayOfWeek dayOfWeek, final LocalTime time) {
        massesByDay.computeIfAbsent(dayOfWeek, v -> new HashMap<>())
                .computeIfAbsent(time, v -> new ArrayList<>())
                .add(massInfo);
    }

    private void addToMassesByDay(Mass mass, DayOfWeek dayOfWeek, LocalTime time) {

        massesByDay.computeIfAbsent(dayOfWeek, v -> new HashMap<LocalTime, List<MassInfo>>()).
                computeIfAbsent(time, v -> new ArrayList<MassInfo>()).add(MassInfoMapper.MAPPER.toMassInfo(mass));
    }


    /**
     * Build schedule for week from provided startWeekDate;
     */
    public MassSchedule createSchedule() {
//        LocalDate startDate = LocalDate.of(getStartWeekDate().getYear(), getStartWeekDate().getMonth(), getStartWeekDate().getDayOfMonth()).minusDays(getStartWeekDate().getDayOfWeek().getValue() - 1)
//                .minusDays(getStartWeekDate().getDayOfWeek().getValue() - 1);

        createSchedule(getStartWeekDate());
        return this;
    }

    private void createSchedule(LocalDate startDate) {
        int counter = 0;

        while (counter < 7) {
            Map<LocalTime, List<MassInfo>> massHours = getMassesByDay().get(startDate.getDayOfWeek());

            if (massHours != null && massHours.size() > 0) {
                final MassDay massDay = new MassDay(startDate);
                massDay.setMassHours(new ArrayList<>(massHours.size()));

                if (onlyValidMasses) {
                    //
                    massHours.forEach((time, massInfos) ->
                            massDay.getMassHours().add(new MassDay.MassHour(time, massInfos)));
                } else {

                    for (Map.Entry<LocalTime, List<MassInfo>> massHourEntry : massHours.entrySet()) {
                        LocalTime hour = massHourEntry.getKey();
                        List<MassInfo> data = massHourEntry.getValue();
                        LocalDate date = massDay.getDate();
                        Predicate<MassInfo> massInfoPredicate = massInfo ->
                        {
                            boolean result = false;
                            if (massInfo.getStartDate() != null) {
                                result = massInfo.getStartDate().isAfter(date);
                            }
                            if (massInfo.getEndDate() != null && !result) {
                                result = massInfo.getEndDate().isBefore(date);
                            }
                            return result;
                        };
                        data.removeIf(massInfoPredicate);
                        if (data.size() > 0) {
                            MassDay.MassHour massHour = new MassDay.MassHour(hour, data);
                            massDay.getMassHours().add(massHour);
                        }
                    }
                }
                if (massDay.getMassHours().size() > 0) {
                    massDay.getMassHours().sort((h1, h2) -> h1.getHour().compareTo(h2.getHour()));
                    schedule.add(massDay);
                }
            }

            startDate = startDate.plusDays(1);
            counter++;
        }
    }


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
