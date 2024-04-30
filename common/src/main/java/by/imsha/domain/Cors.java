package by.imsha.domain;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@Builder
public class Cors {

    @Id
    private String id;

    @NotEmpty(message = "CORS.001")
    @Indexed(unique = true)
    private String origin;
}