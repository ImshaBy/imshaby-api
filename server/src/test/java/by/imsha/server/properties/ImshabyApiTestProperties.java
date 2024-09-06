package by.imsha.server.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "imshaby-api")
public record ImshabyApiTestProperties(String baseUri, String basePath) {
}
