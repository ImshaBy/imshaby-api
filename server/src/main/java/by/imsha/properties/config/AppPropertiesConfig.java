package by.imsha.properties.config;

import by.imsha.properties.ImshaProperties;
import by.imsha.properties.PasswordlessApiProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppPropertiesConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.cors")
    public DynamicCorsConfiguration corsConfiguration() {
        return new DynamicCorsConfiguration();
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
