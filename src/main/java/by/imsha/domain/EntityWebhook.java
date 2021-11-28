package by.imsha.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Builder
@Data
@Document(collection = "hook")
public class EntityWebhook {
    @Id
    private String id;

    private String type;

    @NotNull
    private String key;

    @NotNull
    private String url;

}
