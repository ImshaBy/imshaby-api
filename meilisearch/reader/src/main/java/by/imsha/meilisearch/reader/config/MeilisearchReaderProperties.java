package by.imsha.meilisearch.reader.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class MeilisearchReaderProperties {
    public static final String PREFIX = "meilisearch.reader";

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
     * UID индекса для чтения
     */
    @NotBlank
    private String indexUid;
}
