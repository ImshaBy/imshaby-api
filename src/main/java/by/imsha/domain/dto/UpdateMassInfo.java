package by.imsha.domain.dto;

import by.imsha.rest.serializers.LocalDateSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author Alena Misan
 */
@Data
public class UpdateMassInfo implements Serializable{
    private String langCode;
    private Integer duration;
    private String notes;
    private int[] days;
    private Boolean online;
    //@JsonIgnore
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate endDate;
    //@JsonIgnore
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate startDate;

    private Long singleStartTimestamp;

    @Pattern(regexp = "^[0-2][0-9]:[0-5][0-9]$")
    private String time;

}
