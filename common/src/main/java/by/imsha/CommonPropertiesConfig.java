package by.imsha;

import by.imsha.properties.CommonProperties;
import by.imsha.properties.FusionauthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Bean
    @ConfigurationProperties("fusionauth")
    @ConditionalOnProperty(name = "fusionauth.url")
    public FusionauthProperties fusionAuthProperties() {
        return new FusionauthProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public FusionauthProperties fusionAuthPropertiesStub() {
        //FIXME для того, чтобы не пришлось конфигурировать fusionAuth там, где он не нужен
        return new FusionauthProperties();
    }
}
