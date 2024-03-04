package by.imsha.domain.dto;

import by.imsha.utils.ServiceUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

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


    @JsonIgnore
    private Integer updatePeriodInDays;
    @JsonIgnore
    private LocalDateTime lastConfirmRelevance;

    public boolean isNeedUpdate() {
        return ServiceUtils.needUpdateFromNow(lastConfirmRelevance, updatePeriodInDays);
    }


}
