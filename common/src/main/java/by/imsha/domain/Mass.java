package by.imsha.domain;

import by.imsha.serializers.CustomLocalDateTimeSerializer;
import by.imsha.validation.mass.MassGroups;
import by.imsha.validation.mass.ValidMassCalendar;
import by.imsha.validation.mass.UniqueMass;
import tools.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.GroupSequence;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO refactor Mass model to have different types of Masses
 */
@Document
@Data
@NoArgsConstructor
@GroupSequence({Mass.class, MassGroups.Duplicate.class})
@UniqueMass(groups = MassGroups.Duplicate.class, message = "MASS.011")
public class Mass {

    @Id
    private String id;

    @NotEmpty(message = "MASS.003")
    private String cityId;

    @NotEmpty(message = "MASS.004")
    private String langCode;

    private Long duration = 3600l;

    private Boolean online;

    private Boolean rorate;

    @NotEmpty(message = "MASS.010")
    @Indexed
    private String parishId;

    private boolean deleted = false;

    private String notes;

    private Map<String, LocalizedMass> localizedInfo = new HashMap<>();

    @LastModifiedDate
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastModifiedDate;

    @NotNull(message = "MASS.015")
    @ValidMassCalendar(message = "MASS.016")
    private CalendarDescription calendar;

    public record CalendarDescription(Instant dtstart, String rrule, List<Instant> exdates) {
        public CalendarDescription {
            exdates = exdates == null ? List.of() : List.copyOf(exdates);
        }

        public CalendarDescription(Instant dtstart, String rrule) {
            this(dtstart, rrule, List.of());
        }
    }

    public Mass(Mass mass) {
        this.langCode = mass.langCode;
        this.cityId = mass.cityId;
        this.duration = mass.duration;
        this.parishId = mass.parishId;
        this.calendar = mass.calendar;
        this.online = mass.online;
        this.rorate = mass.rorate;
        this.notes = mass.notes;
        this.deleted = mass.deleted;
        this.localizedInfo = mass.localizedInfo == null ? null : new HashMap<>(mass.localizedInfo);
    }
}
