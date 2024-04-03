package by.imsha.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotEmpty;

@Document
@Data
@Builder
public class Cors {

    @NotEmpty(message = "CORS.001")
    @Indexed(unique = true)
    private String origin;
}