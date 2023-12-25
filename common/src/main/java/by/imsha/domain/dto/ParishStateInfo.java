package by.imsha.domain.dto;

import by.imsha.domain.Parish;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

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
