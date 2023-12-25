package by.imsha.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MassFilterValue {
    private final MassFilterType type;
    private final String value;
    private final String name;
}
