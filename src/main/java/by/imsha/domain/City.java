package by.imsha.domain;

import by.imsha.utils.ServiceUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Represents city class
 *
 */
@Document
@ApiModel
@Getter
@Setter
public class City {

    @Id
    private String id;

    @NotNull
    @NotEmpty
    @Indexed(unique = true)
    private String name;

//    @JsonIgnore
    @ApiModelProperty(value = "key <*> is language code")
    private Map<String, LocalizedBaseInfo> localizedInfo = new HashMap<>();


    public String getLocalizedName() {
        LocalizedBaseInfo localizedBaseInfo = getLocalizedInfo().get(ServiceUtils.fetchUserLangFromHttpRequest());
        String calculatedName = name;
        if(localizedBaseInfo != null){
            calculatedName = ((LocalizedCity) localizedBaseInfo).getName();
        }
        return calculatedName;
    }



    public City(String name) {
        this.name = name;
    }

    public City() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof City)) return false;

        City city = (City) o;

        if (!name.equals(city.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
