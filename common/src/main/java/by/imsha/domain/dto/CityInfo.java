package by.imsha.domain.dto;

import by.imsha.serializers.TrimStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;


/**
 * @author Alena Misan
 */
@Data
public class CityInfo {
    private String name;
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String key;
}
