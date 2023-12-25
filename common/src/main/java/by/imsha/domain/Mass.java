package by.imsha.domain;

import by.imsha.serializers.CustomLocalDateTimeSerializer;
import by.imsha.serializers.LocalDateDeserializer;
import by.imsha.serializers.LocalDateSerializer;
import by.imsha.utils.ServiceUtils;
import by.imsha.validation.common.ComparableFieldsValid;
import by.imsha.validation.mass.MassDaysValid;
import by.imsha.validation.mass.MassGroupSequenceProvider;
import by.imsha.validation.mass.MassGroups.Duplicate;
import by.imsha.validation.mass.MassGroups.Periodic;
import by.imsha.validation.mass.MassGroups.Single;
import by.imsha.validation.mass.UniqueMass;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static by.imsha.validation.common.ComparableFieldsValid.Condition.LESS_OR_EQUALS;

/**
 * TODO refactor Mass model to have different types of Masses
 */
@Document
@CompoundIndexes(
        value = {@CompoundIndex(name = "unique_mass_index_with_dates", def = "{'time': 1, 'days': 1, 'singleStartTimestamp':1, 'parishId':1, 'startDate':1, 'endDate':1}", unique = true)}

)
@Data
@NoArgsConstructor
@GroupSequenceProvider(MassGroupSequenceProvider.class)
@ComparableFieldsValid(fields = {"startDate", "endDate"}, condition = LESS_OR_EQUALS, groups = Periodic.class, message = "MASS.001")
@MassDaysValid(groups = Periodic.class, message = "MASS.002")
@UniqueMass(groups = Duplicate.class, message = "MASS.011")
public class Mass {

    @Id
    private String id;

    @NotEmpty(message = "MASS.003")
    private String cityId;

    @NotEmpty(message = "MASS.004")
    private String langCode;

    private Long duration = 3600l;

    @Indexed
    @Pattern(groups = Periodic.class, regexp = "([01][0-9]|2[0-3]):[0-5][0-9]", message = "MASS.005")
    @NotNull(groups = Periodic.class, message = "MASS.006")
    @Null(groups = Single.class, message = "MASS.007")
    private String time;

    @Indexed
    @NotEmpty(groups = Periodic.class, message = "MASS.008")
    @Size(max = 0, groups = Single.class, message = "MASS.009")
    private int[] days;

    private Boolean online;

    private Boolean rorate;

    @NotEmpty(message = "MASS.010")
    @Indexed
    private String parishId;

    private boolean deleted = false;

    private String notes;

    private Map<String, LocalizedMass> localizedInfo = new HashMap<>();

    private long singleStartTimestamp;

    @LastModifiedDate
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastModifiedDate;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate startDate;


    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate endDate;

    public Mass(String langCode, String cityId, long duration, String parishId, String time, long start, int[] days,
                LocalDate startDate, LocalDate endDate, Boolean online, Boolean rorate, String notes, boolean deleted,
                Map<String, LocalizedMass> localizedInfo) {
        this.langCode = langCode;
        this.cityId = cityId;
        this.duration = duration;
        this.parishId = parishId;
        this.time = time;
        this.singleStartTimestamp = start;
        if (days != null) {
            this.days = Arrays.copyOf(days, days.length);
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.online = online;
        this.rorate = rorate;
        this.notes = notes;
        this.deleted = deleted;
        this.localizedInfo = new HashMap<>(localizedInfo);
    }

    public Mass(Mass mass) {
        this(mass.langCode, mass.cityId, mass.duration, mass.parishId, mass.time, mass.singleStartTimestamp, mass.days,
                mass.startDate, mass.endDate, mass.online, mass.rorate, mass.notes, mass.deleted, mass.localizedInfo);
    }

    public boolean isPeriodic() {
        return this.getSingleStartTimestamp() == 0;
    }

    public Mass asPeriodic() {
        if (singleStartTimestamp == 0) {
            return this;
        }
        Mass periodicMass = new Mass(this);
        LocalDateTime localDateTime = ServiceUtils.timestampToLocalDate(singleStartTimestamp);
        periodicMass.time = localDateTime.toLocalTime().toString();
        periodicMass.days = new int[1];
        periodicMass.days[0] = localDateTime.getDayOfWeek().getValue();
        periodicMass.startDate = periodicMass.endDate = localDateTime.toLocalDate();
        periodicMass.singleStartTimestamp = 0;
        return periodicMass;
    }

}
