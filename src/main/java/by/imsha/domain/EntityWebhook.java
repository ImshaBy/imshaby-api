package by.imsha.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

@Builder
@Data
@Document(collection = "hook")
@CompoundIndexes(
        value = {@CompoundIndex(name = "unique_hook_index", def = "{'key': 1, 'type': 1}", unique = true)}
)
public class EntityWebhook {
    @Id
    private String id;

    private String type;

    @NotNull
    private String key;

    @NotNull
    private String url;
}
