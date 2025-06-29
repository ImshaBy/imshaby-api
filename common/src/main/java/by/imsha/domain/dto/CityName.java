package by.imsha.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;


@Data
@Builder
public class CityName implements Serializable {
    private String name;
}
