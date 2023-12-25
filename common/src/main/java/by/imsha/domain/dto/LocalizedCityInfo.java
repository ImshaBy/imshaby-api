package by.imsha.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author Alena Misan
 */
@Getter
@Setter
public class LocalizedCityInfo implements Serializable {

    @NotEmpty(message = "CITY.001")
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalizedCityInfo)) return false;

        LocalizedCityInfo cityInfo = (LocalizedCityInfo) o;

        return name != null ? name.equals(cityInfo.name) : cityInfo.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
