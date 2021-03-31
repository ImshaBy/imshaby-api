package by.imsha.domain;

import by.imsha.rest.serializers.CustomLocalDateTimeSerializer;
import by.imsha.rest.serializers.LocalDateDeserializer;
import by.imsha.rest.serializers.LocalDateSerializer;
import by.imsha.service.MassService;
import by.imsha.utils.Constants;
import by.imsha.utils.ServiceUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO refactor Mass model to have different types of Masses
 */
@Document
@CompoundIndexes(
        value = {@CompoundIndex(name = "unique_mass_index_with_dates", def = "{'time': 1, 'days': 1, 'singleStartTimestamp':1, 'parishId':1, 'startDate':1, 'endDate':1}", unique = true)}

)
@Data
public class Mass {

    @Id
    private String id;

    //    @ApiObjectField(description = "City ID.", required = true)
    @NotNull
    @NotEmpty
    private String cityId;


    //    @ApiObjectField(description = "Language code of provided mass. Available codes are presented in ISO 639-2 Language Code List.", required = true)
    @NotNull
    @NotEmpty
    private String langCode;

    //    @ApiObjectField(description = "Duration of mass in ms, default value = 3600 (1 hour)",  required = false)
//    @NotNull
    private Long duration = 3600l;

//    @ApiObjectField(description = "Time of regular mass, that is defined throw time and days.", required = false)
//    @JsonFormat(pattern = "KK:mm")
//    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
//    @JsonSerialize(using = LocalDateTimeSerializer.class)

    @Indexed
    @Pattern(regexp = "^[0-2][0-9]:[0-5][0-9]$")
    private String time;

    @Indexed
    private int[] days;

    private Boolean online;


    @NotNull
    @NotEmpty
    @Indexed
    private String parishId;

    private boolean deleted = false;

    private String notes;

    private Map<String, LocalizedMass> localizedInfo = new HashMap<>();

    public Map<String, LocalizedMass> getLocalizedInfo() {
        return localizedInfo;
    }

    public void setLocalizedInfo(Map<String, LocalizedMass> localizedInfo) {
        this.localizedInfo = localizedInfo;
    }

    //    @ApiObjectField(description = "Start time for non regular mass, that occurs and is defined only once", required = false)
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


    public Mass() {
    }

    @AssertTrue(message = "Only one of fields have to be populated: time or singleStartTimestamp")
    private boolean isValid() {
        return MassService.isMassTimeConfigIsValid(this);
    }

    @AssertTrue(message = "Please specify 'days' for scheduled mass (you already specified field 'time').")
    private boolean isValidScheduledMassEmptyDays() {
        return MassService.isScheduleMassDaysIsCorrect(this);
    }

    @AssertTrue(message = "Please specify 'time' for scheduled mass (you already specified field 'days').")
    private boolean isValidScheduledMassEmptyTime() {
        return MassService.isScheduleMassTimeIsCorrect(this);
    }



    public Mass(String langCode, String cityId, long duration, String parishId, String time, long start, int[] days) {
        this.langCode = langCode;
        this.cityId = cityId;
        this.duration = duration;
        this.parishId = parishId;
        this.time = time;
        this.singleStartTimestamp = start;
        this.days = days;
    }

    public Mass(Mass mass) {
        this(mass.langCode, mass.cityId, mass.duration, mass.parishId, mass.time, mass.singleStartTimestamp,
                Arrays.copyOf(mass.days, mass.days.length));
        localizedInfo = new HashMap<>(mass.localizedInfo);
    }







    public String getNotes() {
        String lang = ServiceUtils.fetchUserLangFromHttpRequest();
        LocalizedMass localizedMass = getLocalizedInfo().get(lang);
        String calculatedNotes = null;
        if(localizedMass != null){
            calculatedNotes =  localizedMass.getNotes();
        }else if(Constants.DEFAULT_LANG.equalsIgnoreCase(lang)){
            calculatedNotes = notes;
        }
        return calculatedNotes;
    }
}
