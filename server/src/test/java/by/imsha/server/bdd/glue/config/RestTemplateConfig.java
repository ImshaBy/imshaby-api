package by.imsha.server.bdd.glue.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static by.imsha.server.bdd.glue.steps.request.HttpRequestData.API_KEY_HEADER_NAME;
import static by.imsha.server.bdd.glue.steps.request.HttpRequestData.INTERNAL_TEST_API_KEY;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate internalRestTemplate(){
        return new RestTemplateBuilder().defaultHeader(API_KEY_HEADER_NAME, INTERNAL_TEST_API_KEY).build();
    }
}