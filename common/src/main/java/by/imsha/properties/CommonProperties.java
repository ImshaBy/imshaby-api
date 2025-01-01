package by.imsha.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

/**
 * Параметры приложения imshaby
 */
@Data
@Validated
public class CommonProperties {

    /**
     * Идентификатор временной зоны
     */
    @NotNull
    private ZoneId zoneId;
}