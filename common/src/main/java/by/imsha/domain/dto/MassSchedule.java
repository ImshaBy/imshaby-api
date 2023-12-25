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

    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate startWeekDate;

    @Getter
    @Setter
    private MassNav nav;

    public MassSchedule(LocalDate startDate) {
        this.startWeekDate = startDate;
        weekMasses = new HashMap<WeekDayTimeKey, List<Mass>>();
        massesByDay = new HashMap<DayOfWeek, Map<LocalTime, List<MassInfo>>>();
        schedule = new ArrayList<MassDay>();
    }

    @JsonIgnore
    private Map<WeekDayTimeKey, List<Mass>> weekMasses;

    private List<MassDay> schedule;

    public List<MassDay> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<MassDay> schedule) {
        this.schedule = schedule;
    }

    @JsonIgnore
    private Map<DayOfWeek, Map<LocalTime, List<MassInfo>>> massesByDay;

    public Map<DayOfWeek, Map<LocalTime, List<MassInfo>>> getMassesByDay() {
        return massesByDay;
    }

    public LocalDate getStartWeekDate() {
        return startWeekDate;
    }

    public Map<WeekDayTimeKey, List<Mass>> getWeekMasses() {
        return weekMasses;
    }

    public void populateContainers(Mass mass, DayOfWeek dayOfWeek, LocalTime time) {
        // addToWeekMasses(mass, dayOfWeek, time);
        addToMassesByDay(mass, dayOfWeek, time);
    }


    private void addToWeekMasses(Mass mass, DayOfWeek dayOfWeek, LocalTime time) {
        WeekDayTimeKey weekTimeKey = new WeekDayTimeKey(dayOfWeek, time);
        weekMasses.computeIfAbsent(weekTimeKey, v -> new ArrayList<Mass>()).add(mass);
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
            MassDay massDay = null;
            Map<LocalTime, List<MassInfo>> massHours = getMassesByDay().get(startDate.getDayOfWeek());
            if (massHours != null && massHours.size() > 0) {
                massDay = new MassDay(startDate);
                massDay.setMassHours(new ArrayList<MassDay.MassHour>(massHours.size()));
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
