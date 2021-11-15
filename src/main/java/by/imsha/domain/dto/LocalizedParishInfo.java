package by.imsha.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class LocalizedParishInfo implements Serializable {
    @NotNull
    @NotEmpty
    private String name;
    private String shortName;
    private String address;
}
