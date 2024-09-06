package by.imsha.domain.dto;

import by.imsha.serializers.TrimStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Alena Misan
 */
@Data
public class ParishInfo implements Serializable {

    private String name;
    private String shortName;
    private String imgPath;
    private LocationInfo gps;
    private String address;
    private Integer updatePeriodInDays;
    private String supportPhone;
    private String email;
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String key;
    private String phone;
    private String lastModifiedEmail;
    private String website;
    private String broadcastUrl;

}
