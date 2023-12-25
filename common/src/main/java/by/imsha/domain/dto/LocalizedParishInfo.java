package by.imsha.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

@Data
public class LocalizedParishInfo implements Serializable {
    @NotEmpty(message = "PARISH.002")
    private String name;
    private String shortName;
    private String address;
}
