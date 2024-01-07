package by.imsha.server.properties.config;

import by.imsha.server.properties.ImshaProperties;
import by.imsha.server.properties.PasswordlessApiProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
public class AppPropertiesConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.cors")
    public CorsConfiguration corsConfiguration() {
        return new CorsConfiguration();
    }

    @Bean
    @ConfigurationProperties(PasswordlessApiProperties.PREFIX)
    public PasswordlessApiProperties passwordlessApiProperties() {
        return new PasswordlessApiProperties();
    }

    @Bean
    @ConfigurationProperties("app.imsha")
    public ImshaProperties imshaProperties() {
        return new ImshaProperties();
    }
}
