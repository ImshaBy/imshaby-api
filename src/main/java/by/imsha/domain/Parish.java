package by.imsha.domain;

import by.imsha.rest.serializers.CustomLocalDateTimeSerializer;
import by.imsha.service.MassService;
import by.imsha.utils.ServiceUtils;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represent Parish class
 */
//@ApiObject(show = true, name = "Parish", description = "Parish object json structure.")
@Document
@Data
public class Parish {

    @Id
    private String id;

    private String imgPath;

    private String broadcastUrl;

    //    @ApiObjectField(description = "Auth0 system user identificator. It is provided only after futhentification in auth0.com with current login API.", required = true)
//    @NotNull
    private String userId;

    //    @ApiObjectField(description = "Name of parish.", required = true)
    @NotNull
    @NotEmpty
    private String name;

    //    @ApiObjectField(description = "Address string of parish (only street and house number).", required = false)
    private String address;

    //    @ApiObjectField(description = "Coordinates of parish in format ##.###### for longitude/latitude", required = true)
//    @NotNull
    private Coordinate gps;

    @Indexed(unique=true)
    private String key;

    private Integer updatePeriodInDays = 14;

    private Map<String, LocalizedBaseInfo> localizedInfo = new HashMap<>();


    private boolean needUpdate;

    public boolean isNeedUpdate() {
        return ServiceUtils.needUpdateFromNow(lastModifiedDate, getUpdatePeriodInDays());
    }

    @JsonSerialize(using= CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastMassActualDate;

    public LocalDateTime getLastMassActualDate() {
        LocalDateTime oldestModifiedMassTimeForParish = MassService.getOldestModifiedMassTimeForParish(this.id);
        LocalDateTime localDateTime = oldestModifiedMassTimeForParish != null ? oldestModifiedMassTimeForParish.plusDays(this.updatePeriodInDays) : null;
        return localDateTime;
    }

    public void setLastMassActualDate(LocalDateTime lastMassActualDate) {
        this.lastMassActualDate = lastMassActualDate;
    }

    @LastModifiedDate
    @JsonSerialize(using = CustomLocalDateTimeSerializer.class)
    private LocalDateTime lastModifiedDate;

    public Parish() {
    }

    public Parish(String userId, String name, Coordinate gps, String cityId, String supportPhone, String email) {
        this.userId = userId;
        this.name = name;
        this.gps = gps;
        this.cityId = cityId;
        this.supportPhone = supportPhone;
        this.email = email;
    }

    //    @ApiObjectField(description = "City ID of parish.", required = true)
    @NotNull
    @NotEmpty
    private String cityId;

    //    @ApiObjectField(description = "Official phone provided by parish; phone available for public audience.", required = false)
    private String phone;

    //    @ApiObjectField(description = "Not available for public audience; used for internal purpose.", required = true)
    @NotNull
    @NotEmpty
    private String supportPhone;

    //    @ApiObjectField(description = "Parish email.", required = true)
    @Email
    @NotNull
    private String email;

    @Email
    private String lastModifiedEmail;



    //    @ApiObjectField(description = "Parish web-site link.", required = false)
    private String website;


    public String getName() {
        LocalizedBaseInfo localizedBaseInfo = getLocalizedInfo().get(ServiceUtils.fetchUserLangFromHttpRequest());
        String calculatedName = name;
        if(localizedBaseInfo != null){
            calculatedName = ((LocalizedParish) localizedBaseInfo).getName();
        }
        return calculatedName;
    }

    public String getAddress() {
        LocalizedBaseInfo localizedBaseInfo = getLocalizedInfo().get(ServiceUtils.fetchUserLangFromHttpRequest());
        String calcAddress = address;
        if(localizedBaseInfo != null){
            calcAddress = ((LocalizedParish)localizedBaseInfo).getAddress();
        }
        return calcAddress;
    }

}
