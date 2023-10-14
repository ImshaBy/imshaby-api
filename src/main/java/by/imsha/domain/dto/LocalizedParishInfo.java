package by.imsha.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;

@Data
public class LocalizedParishInfo implements Serializable {
    @NotEmpty(message = "PARISH.002")
    private String name;
    private String shortName;
    private String address;
}
