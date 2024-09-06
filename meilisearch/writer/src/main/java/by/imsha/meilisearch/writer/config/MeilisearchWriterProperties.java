package by.imsha.meilisearch.writer.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class MeilisearchWriterProperties {
    public static final String PREFIX = "meilisearch.writer";

    /**
     * URL хоста meilisearch
     */
    @NotBlank
    private String hostUrl;
    /**
     * API-ключ для работы с meilisearch
     */
    @NotBlank
    private String apiKey;
    /**
     * UID индекса для записи
     */
    @NotBlank
    private String indexUid;
}
