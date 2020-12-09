package by.imsha.domain;

import by.imsha.rest.serializers.CustomLocalDateTimeSerializer;
import by.imsha.service.MassService;
import by.imsha.utils.ServiceUtils;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModel;
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
@ApiModel
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

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Integer getUpdatePeriodInDays() {
        return updatePeriodInDays;
    }


    /**
     * Not needed to set update period more than 28 days;
     */
    public void setUpdatePeriodInDays(Integer updatePeriodInDays) {
        this.updatePeriodInDays = updatePeriodInDays;
    }

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
    @Indexed(unique=true)
    private String email;

    @Email
    private String lastModifiedEmail;



    //    @ApiObjectField(description = "Parish web-site link.", required = false)
    private String website;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parish)) return false;

        Parish parish = (Parish) o;

        if (!cityId.equals(parish.cityId)) return false;
        if (!key.equals(parish.key)) return false;
        if (!email.equals(parish.email)) return false;
        if (!gps.equals(parish.gps)) return false;
        if (!id.equals(parish.id)) return false;
        if (imgPath != null ? !imgPath.equals(parish.imgPath) : parish.imgPath != null) return false;
        if (!name.equals(parish.name)) return false;
        if (!userId.equals(parish.userId)) return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (gps != null ? gps.hashCode() : 0);
        result = 31 * result + (cityId != null ? cityId.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (phone != null ? phone.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (imgPath != null ? imgPath.hashCode() : 0);
        result = 31 * result + (website != null ? website.hashCode() : 0);
        return result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        LocalizedBaseInfo localizedBaseInfo = getLocalizedInfo().get(ServiceUtils.fetchUserLangFromHttpRequest());
        String calculatedName = name;
        if(localizedBaseInfo != null){
            calculatedName = ((LocalizedParish) localizedBaseInfo).getName();
        }
        return calculatedName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        LocalizedBaseInfo localizedBaseInfo = getLocalizedInfo().get(ServiceUtils.fetchUserLangFromHttpRequest());
        String calcAddress = address;
        if(localizedBaseInfo != null){
            calcAddress = ((LocalizedParish)localizedBaseInfo).getAddress();
        }
        return calcAddress;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Coordinate getGps() {
        return gps;
    }

    public void setGps(Coordinate gps) {
        this.gps = gps;
    }

    public String getCityId() {
        return cityId;
    }

    public void setCityId(String cityId) {
        this.cityId = cityId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSupportPhone() {
        return supportPhone;
    }

    public void setSupportPhone(String supportPhone) {
        this.supportPhone = supportPhone;
    }

    @Email
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public String getLastModifiedEmail() {
        return lastModifiedEmail;
    }

    public void setLastModifiedEmail(String lastModifiedEmail) {
        this.lastModifiedEmail = lastModifiedEmail;
    }

    public Map<String, LocalizedBaseInfo> getLocalizedInfo() {
        return localizedInfo;
    }

    public void setLocalizedInfo(Map<String, LocalizedBaseInfo> localizedInfo) {
        this.localizedInfo = localizedInfo;
    }

    public String getBroadcastUrl() {
        return broadcastUrl;
    }

    public void setBroadcastUrl(String broadcastUrl) {
        this.broadcastUrl = broadcastUrl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
