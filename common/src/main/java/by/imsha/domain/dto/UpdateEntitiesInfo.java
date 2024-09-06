package by.imsha.domain.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * @author Alena Misan
 */
public class UpdateEntitiesInfo implements Serializable {
    public enum STATUS {
        UPDATED, DELETED, ERROR
    }

    private List<String> entities;

    private String status;

    public UpdateEntitiesInfo(List<String> id, STATUS status) {
        this.entities = id;
        this.status = status.toString();
    }

    public UpdateEntitiesInfo() {
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

        if (o == null || getClass() != o.getClass()) return false;

        UpdateEntitiesInfo that = (UpdateEntitiesInfo) o;

        return new EqualsBuilder()
                .append(getEntities(), that.getEntities())
                .append(getStatus(), that.getStatus())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getEntities())
                .append(getStatus())
                .toHashCode();
    }

    public List<String> getEntities() {
        return entities;
    }

    public void setEntities(List<String> entities) {
        this.entities = entities;
    }
}
