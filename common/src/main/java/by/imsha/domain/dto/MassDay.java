package by.imsha.domain.dto;

import by.imsha.serializers.LocalDateSerializer;
import by.imsha.serializers.LocalTimeSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * @author Andrei Misan
 */
public class MassDay implements Serializable {

    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate date;

    private List<MassHour> massHours;

    MassDay(LocalDate date) {
        this.date = date;
    }

    MassDay() {
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MassDay)) return false;

        MassDay massDay = (MassDay) o;

        if (!date.equals(massDay.date)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return date.hashCode();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<MassHour> getMassHours() {
        return massHours;
    }

    public void setMassHours(List<MassHour> massHours) {
        this.massHours = massHours;
    }

    static class MassHour {

        @JsonSerialize(using = LocalTimeSerializer.class)
        private LocalTime hour;
        private List<MassInfo> data;

        @JsonIgnore
        private boolean active;

        MassHour(LocalTime hour, List<MassInfo> data) {
            this.hour = hour;
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MassHour)) return false;

            MassHour massHour = (MassHour) o;

            if (!hour.equals(massHour.hour)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return hour.hashCode();
        }

        public LocalTime getHour() {
            return hour;
        }

        public void setHour(LocalTime hour) {
            this.hour = hour;
        }

        public List<MassInfo> getData() {
            return data;
        }

        public void setData(List<MassInfo> data) {
            this.data = data;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
