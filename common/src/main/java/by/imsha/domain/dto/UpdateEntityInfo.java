package by.imsha.domain.dto;

import java.io.Serializable;

/**
 * @author Alena Misan
 */
public class UpdateEntityInfo implements Serializable {
    public enum STATUS {
        UPDATED, DELETED, CREATED, ERROR
    }
    private String id;
    private String status;

    public UpdateEntityInfo(String id, STATUS status) {
        this.id = id;
        this.status = status.toString();
    }
    public UpdateEntityInfo(){
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateEntityInfo)) return false;

        UpdateEntityInfo that = (UpdateEntityInfo) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return status != null ? status.equals(that.status) : that.status == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }
}
