package by.imsha.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * Параметры для работы с fusionAuth
 */
@Data
@Validated
public class FusionAuthProperties {

    @NotEmpty
    private String authorizationToken;

    @NotNull
    private Integer userSearchPagination;

    @NotEmpty
    private String applicationId;

    @NotEmpty
    private String hostUrl;
}
