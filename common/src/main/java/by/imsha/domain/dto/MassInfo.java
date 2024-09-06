package by.imsha.domain.dto;

import by.imsha.serializers.CustomLocalDateTimeSerializer;
import by.imsha.serializers.LocalDateSerializer;
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

    @Deprecated
    public boolean isNeedUpdate() {
        return parish.isNeedUpdate();
    }

}
