package by.imsha;

import by.imsha.validation.ConstraintViolationPayloadBase64Coder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfiguration {

    @Bean
    public ConstraintViolationPayloadBase64Coder constraintViolationPayloadBase64Coder(final ObjectMapper objectMapper) {
        return new ConstraintViolationPayloadBase64Coder(objectMapper);
    }
}
