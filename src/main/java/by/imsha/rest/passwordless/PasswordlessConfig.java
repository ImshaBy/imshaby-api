package by.imsha.rest.passwordless;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PasswordlessConfig {

    @Bean
    RestTemplate passwordlessSecureRestTemplate(RestTemplateBuilder restTemplateBuilder,
                                                PasswordlessApiProperties passwordlessApiProperties) {
        return restTemplateBuilder
                .defaultHeader(HttpHeaders.AUTHORIZATION, passwordlessApiProperties.getApiKey())
                .build();
    }

    @Bean
    RestTemplate passwordlessPublicRestTemplate(RestTemplateBuilder restTemplateBuilder,
                                                PasswordlessApiProperties passwordlessApiProperties) {
        return restTemplateBuilder.build();
    }
}
