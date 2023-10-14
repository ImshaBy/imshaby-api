package by.imsha.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class WebHookInfo {

    @NotNull(message = "WEBHOOK.001")
    private String key;

    @NotNull(message = "WEBHOOK.002")
    private String url;
}
