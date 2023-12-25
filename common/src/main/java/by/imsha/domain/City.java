package by.imsha.domain;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents city class
 */
@Document
@Data
@Builder
public class City {

    @Id
    private String id;

    @Indexed(unique = true)
    private String key;

    @NotEmpty(message = "CITY.001")
    @Indexed(unique = true)
    private String name;

    @Builder.Default
    private Map<String, LocalizedBaseInfo> localizedInfo = new HashMap<>();
}
