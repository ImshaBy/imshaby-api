package by.imsha.domain;

import by.imsha.serializers.CustomLocalDateTimeSerializer;
import by.imsha.serializers.LocalDateDeserializer;
import by.imsha.serializers.LocalDateSerializer;
import by.imsha.utils.ServiceUtils;
import by.imsha.validation.common.ComparableFieldsValid;
import by.imsha.validation.mass.DeprecatedMassDaysValid;
import by.imsha.validation.mass.DeprecatedMassGroupSequenceProvider;
import by.imsha.validation.mass.DeprecatedMassStartWeekdayValid;
import by.imsha.validation.mass.MassGroups.Periodic;
import by.imsha.validation.mass.MassGroups.Single;
import by.imsha.validation.mass.MassGroups.StartWeekday;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.group.GroupSequenceProvider;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static by.imsha.validation.common.ComparableFieldsValid.Condition.LESS_OR_EQUALS;

/** Представление мессы для сервисов v1. */
@Data
@NoArgsConstructor
@GroupSequenceProvider(DeprecatedMassGroupSequenceProvider.class)
@ComparableFieldsValid(fields = {"startDate", "endDate"}, condition = LESS_OR_EQUALS,
        groups = Periodic.class, message = "MASS.001")
@DeprecatedMassDaysValid(groups = Periodic.class, message = "MASS.002")
@DeprecatedMassStartWeekdayValid(groups = StartWeekday.class, message = "MASS.013")
public class DeprecatedMass {
    private String id;
    @NotEmpty(message = "MASS.003")
    private String cityId;
    @NotEmpty(message = "MASS.004")
    private String langCode;
    private Long duration = 3600L;
    @Pattern(groups = Periodic.class, regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "MASS.005")
    @NotNull(groups = Periodic.class, message = "MASS.006")
    @Null(groups = Single.class, message = "MASS.007")
    private String time;
    @NotEmpty(groups = Periodic.class, message = "MASS.008")
    @Size(max = 0, groups = Single.class, message = "MASS.009")
    private int[] days;
    private Boolean online;
    private Boolean rorate;
    @NotEmpty(message = "MASS.010")
    private String parishId;
    private boolean deleted;
    private String notes;
    private Map<String, LocalizedMass> localizedInfo = new HashMap<>();
    private long singleStartTimestamp;

    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastModifiedDate;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @NotNull(groups = Periodic.class, message = "MASS.014")
    private LocalDate startDate;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate endDate;

    public boolean isPeriodic() {
        return singleStartTimestamp == 0;
    }

    public DeprecatedMass asPeriodic() {
        if (isPeriodic()) {
            return this;
        }
        var start = ServiceUtils.timestampToLocalDate(singleStartTimestamp);
        var periodic = new DeprecatedMass();
        periodic.cityId = cityId;
        periodic.langCode = langCode;
        periodic.duration = duration;
        periodic.parishId = parishId;
        periodic.online = online;
        periodic.rorate = rorate;
        periodic.deleted = deleted;
        periodic.notes = notes;
        periodic.localizedInfo = localizedInfo == null ? null : new HashMap<>(localizedInfo);
        periodic.time = start.toLocalTime().toString();
        periodic.days = new int[]{start.getDayOfWeek().getValue()};
        periodic.startDate = start.toLocalDate();
        periodic.endDate = periodic.startDate;
        return periodic;
    }
}
