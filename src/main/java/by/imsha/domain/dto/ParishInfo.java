package by.imsha.domain.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * @author Alena Misan
 */
public class ParishInfo implements Serializable {

    private String name;
    private String imgPath;
    private LocationInfo gps;
    private String address;
    private Integer updatePeriodInDays;
    private String supportPhone;
    private String email;
    private String key;
    private String phone;
    private String lastModifiedEmail;
    private String website;
    private String broadcastUrl;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ParishInfo that = (ParishInfo) o;

        return new EqualsBuilder()
                .append(updatePeriodInDays, that.updatePeriodInDays)
                .append(name, that.name)
                .append(imgPath, that.imgPath)
                .append(gps, that.gps)
                .append(address, that.address)
                .append(key, that.key)
                .append(supportPhone, that.supportPhone)
                .append(email, that.email)
                .append(phone, that.phone)
                .append(lastModifiedEmail, that.lastModifiedEmail)
                .append(website, that.website)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(imgPath)
                .append(gps)
                .append(address)
                .append(updatePeriodInDays)
                .append(supportPhone)
                .append(email)
                .append(phone)
                .append(key)
                .append(lastModifiedEmail)
                .append(website)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("imgPath", imgPath)
                .append("gps", gps)
                .append("address", address)
                .append("updatePeriodInDays", updatePeriodInDays)
                .append("supportPhone", supportPhone)
                .append("email", email)
                .append("key", key)
                .append("phone", phone)
                .append("lastModifiedEmail", lastModifiedEmail)
                .append("website", website)
                .toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public LocationInfo getGps() {
        return gps;
    }

    public void setGps(LocationInfo gps) {
        this.gps = gps;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSupportPhone() {
        return supportPhone;
    }

    public void setSupportPhone(String supportPhone) {
        this.supportPhone = supportPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLastModifiedEmail() {
        return lastModifiedEmail;
    }

    public void setLastModifiedEmail(String lastModifiedEmail) {
        this.lastModifiedEmail = lastModifiedEmail;
    }

    public String getWebsite() {
        return website;
    }

    public String getBroadcastUrl() {
        return broadcastUrl;
    }

    public void setBroadcastUrl(String broadcastUrl) {
        this.broadcastUrl = broadcastUrl;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Integer getUpdatePeriodInDays() {
        return updatePeriodInDays;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setUpdatePeriodInDays(Integer updatePeriodInDays) {
        this.updatePeriodInDays = updatePeriodInDays;
    }
}
