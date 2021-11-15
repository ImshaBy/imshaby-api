package by.imsha.domain.dto;

import by.imsha.rest.serializers.CustomLocalDateTimeSerializer;
import by.imsha.rest.serializers.LocalDateSerializer;
import by.imsha.utils.ServiceUtils;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Alena Misan
 */
@Data
public class MassInfo implements Serializable{
    private String id;
    private String langCode;
    private MassParishInfo parish;
    private Integer duration;
    private String info;
    private int[] days;
    private boolean online;
    private boolean rorate;
    //@JsonIgnore
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate endDate;
    //@JsonIgnore
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate startDate;


    @JsonSerialize(using= CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastModifiedDate;

    public boolean isNeedUpdate() {
        return ServiceUtils.needUpdateFromNow(lastModifiedDate, parish.getUpdatePeriodInDays()) && parish.isNeedUpdate();
    }

}
