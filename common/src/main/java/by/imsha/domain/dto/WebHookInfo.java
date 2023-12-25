package by.imsha.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WebHookInfo {

    @NotNull(message = "WEBHOOK.001")
    private String key;

    @NotNull(message = "WEBHOOK.002")
    private String url;
}
