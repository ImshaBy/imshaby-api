package by.imsha.domain;

import by.imsha.utils.ServiceUtils;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Represents city class
 *
 */
@Document
@Data
@Builder
public class City {

    @Id
    private String id;

    @Indexed(unique = true)
    private String key;

    @NotNull
    @NotEmpty
    @Indexed(unique = true)
    private String name;

//    @JsonIgnore
    @Builder.Default
    private Map<String, LocalizedBaseInfo> localizedInfo = new HashMap<>();


    public String getLocalizedName() {
        LocalizedBaseInfo localizedBaseInfo = getLocalizedInfo().get(ServiceUtils.fetchUserLangFromHttpRequest());
        String calculatedName = name;
        if(localizedBaseInfo != null){
            calculatedName = ((LocalizedCity) localizedBaseInfo).getName();
        }
        return calculatedName;
    }

}
