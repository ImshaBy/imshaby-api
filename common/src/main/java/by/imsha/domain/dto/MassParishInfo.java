package by.imsha.domain.dto;

import by.imsha.serializers.CustomLocalDateTimeSerializer;
import by.imsha.utils.ServiceUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * @author Alena Misan
 */
@Data
public class MassParishInfo implements Serializable {
    private String parishId;
    private String name;
    private String shortName;
    private String imgPath;
    private LocationInfo gps;
    private String address;
    private String broadcastUrl;
    private Boolean volunteerNeeded;


    private Integer updatePeriodInDays;
    @JsonSerialize(using= CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastConfirmRelevance;
    @JsonIgnore
    private LocalDateTime lastModifiedDate;

    public boolean isNeedUpdate() {
        return ServiceUtils.needUpdateFromNow(Optional.ofNullable(lastConfirmRelevance).orElse(lastModifiedDate), updatePeriodInDays);
    }


}
