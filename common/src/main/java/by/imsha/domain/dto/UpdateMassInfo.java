package by.imsha.domain.dto;

import by.imsha.serializers.LocalDateSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author Alena Misan
 */
@Data
public class UpdateMassInfo implements Serializable {
    private String langCode;
    private Integer duration;
    private String notes;
    private int[] days;
    private Boolean online;
    private Boolean rorate;
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate endDate;
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate startDate;

    private Long singleStartTimestamp;

    private String time;

}
