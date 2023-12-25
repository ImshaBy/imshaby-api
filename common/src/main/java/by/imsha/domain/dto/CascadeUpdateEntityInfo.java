package by.imsha.domain.dto;

import java.util.List;
import java.util.Objects;

/**
 * @author Alena Misan
 */

public class CascadeUpdateEntityInfo extends UpdateEntityInfo {



    List<UpdateEntityInfo> relatedEntities;

    public List<UpdateEntityInfo> getRelatedEntities() {
        return relatedEntities;
    }

    public void setRelatedEntities(List<UpdateEntityInfo> relatedEntities) {
        this.relatedEntities = relatedEntities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CascadeUpdateEntityInfo)) return false;
        if (!super.equals(o)) return false;
        CascadeUpdateEntityInfo that = (CascadeUpdateEntityInfo) o;
        return Objects.equals(relatedEntities, that.relatedEntities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), relatedEntities);
    }
}
