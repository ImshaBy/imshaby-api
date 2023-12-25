package by.imsha.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

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

    //TODO аннотации стоят, но по факту не работают, проверки лежат на web слое
    @NotNull(message = "WEBHOOK.001")
    private String key;
    //TODO аннотации стоят, но по факту не работают, проверки лежат на web слое
    @NotNull(message = "WEBHOOK.002")
    private String url;
}
