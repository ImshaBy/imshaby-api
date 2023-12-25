package by.imsha.exception.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    ZonedDateTime timestamp;

    RequestInfo requestInfo;

    @JsonProperty("errors")
    @Singular("error")
    List<FieldError> errors;

    @Value
    @Builder
    public static class FieldError {

        String field;
        String code;
        Object payload;
        String message;
    }
}
