package by.imsha;

import by.imsha.properties.CommonProperties;
import by.imsha.properties.FusionAuthProperties;
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
    @ConfigurationProperties("fusion-auth")
    @ConditionalOnProperty(name = "fusion-auth.authorization-token")
    public FusionAuthProperties fusionAuthProperties() {
        return new FusionAuthProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public FusionAuthProperties fusionAuthPropertiesStub() {
        //FIXME для того, чтобы не пришлось конфигурировать fusionAuth там, где он не нужен
        return new FusionAuthProperties();
    }
}
