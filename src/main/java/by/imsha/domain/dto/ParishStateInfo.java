package by.imsha.domain.dto;

import by.imsha.domain.Parish;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotNull;

/**
 * Информация о состоянии парафии
 */
@Value
@Builder
@Jacksonized
public class ParishStateInfo {

    /**
     * Состояние парафии
     */
    @NotNull(message = "PARISH.401")
    Parish.State state;
}
