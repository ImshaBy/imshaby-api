package by.imsha.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Alena Misan
 */
@Data
public class MassParishInfo implements Serializable {
    private String parishId;
    private String name;
    private String imgPath;
    private LocationInfo gps;
    private String address;
    private boolean needUpdate;
    private String broadcastUrl;


    @JsonIgnore
    private Integer updatePeriodInDays;

}
