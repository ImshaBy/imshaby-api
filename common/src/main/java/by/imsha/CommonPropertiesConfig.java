package by.imsha;

import by.imsha.properties.CommonProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonPropertiesConfig {

    @Bean
    @ConfigurationProperties("app.imsha")
    public CommonProperties commonProperties() {
        return new CommonProperties();
    }
}
