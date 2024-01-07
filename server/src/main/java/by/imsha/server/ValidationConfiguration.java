package by.imsha.server;

import by.imsha.validation.ConstraintViolationPayloadBase64Coder;
import by.imsha.validation.mass.UniqueMassValidatorHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfiguration {

    @Bean
    public ConstraintViolationPayloadBase64Coder constraintViolationPayloadBase64Coder(final ObjectMapper objectMapper) {
        return new ConstraintViolationPayloadBase64Coder(objectMapper);
    }

    @Bean
    public UniqueMassValidatorHelper uniqueMassValidatorHelper() {
        return new UniqueMassValidatorHelper();
    }
}
