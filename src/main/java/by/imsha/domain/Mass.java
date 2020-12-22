package by.imsha.domain;

import by.imsha.rest.serializers.CustomLocalDateTimeSerializer;
import by.imsha.rest.serializers.LocalDateDeserializer;
import by.imsha.rest.serializers.LocalDateSerializer;
import by.imsha.service.MassService;
import by.imsha.utils.Constants;
import by.imsha.utils.ServiceUtils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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


    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mass)) return false;

        Mass mass = (Mass) o;

        if (duration != mass.duration) return false;
        if (singleStartTimestamp != mass.singleStartTimestamp) return false;
        if (!Arrays.equals(days, mass.days)) return false;
        if (!langCode.equals(mass.langCode)) return false;
        if (!parishId.equals(mass.parishId)) return false;
        if (time != null ? !time.equals(mass.time) : mass.time != null) return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = langCode != null ? langCode.hashCode() : 0;
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (days != null ? Arrays.hashCode(days) : 0);
        result = 31 * result + (parishId != null ? parishId.hashCode() : 0);
        result = 31 * result + (int) (singleStartTimestamp ^ (singleStartTimestamp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Mass{" +
                "id='" + id + '\'' +
                ", cityId='" + cityId + '\'' +
                ", langCode='" + langCode + '\'' +
                ", duration=" + duration +
                ", time='" + time + '\'' +
                ", days=" + Arrays.toString(days) +
                ", parishId='" + parishId + '\'' +
                ", deleted=" + deleted +
                ", notes='" + notes + '\'' +
                ", singleStartTimestamp=" + singleStartTimestamp +
                ", lastModifiedDate=" + lastModifiedDate +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLangCode() {
        return langCode;
    }

    public void setLangCode(String langCode) {
        this.langCode = langCode;
    }


    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getParishId() {
        return parishId;
    }

    public void setParishId(String parishId) {
        this.parishId = parishId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getSingleStartTimestamp() {
        return singleStartTimestamp;
    }

    public void setSingleStartTimestamp(long singleStartTimestamp) {
        this.singleStartTimestamp = singleStartTimestamp;
    }

    public int[] getDays() {
        return days;
    }

    public void setDays(int[] days) {
        this.days = days;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
