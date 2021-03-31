package by.imsha.domain.dto;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 * @author Alena Misan
 */
@Data
public class CityInfo {
    private String name;
    private String key;
}
