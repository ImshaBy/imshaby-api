package by.imsha.rest.passwordless;

import by.imsha.properties.PasswordlessApiProperties;
import by.imsha.rest.passwordless.send.CodeSender;
import by.imsha.rest.passwordless.send.ConsoleCodeSender;
import by.imsha.rest.passwordless.send.EmailCodeSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
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
    RestTemplate passwordlessPublicRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean
    @Conditional(ConsoleSenderCondition.class)
    public CodeSender consoleCodeSender() {
        return new ConsoleCodeSender();
    }

    @Bean
    @ConditionalOnMissingBean
    public CodeSender codeSender(PasswordlessApiProperties passwordlessApiProperties,
                                 @Qualifier("passwordlessPublicRestTemplate") RestTemplate restTemplate) {
        return new EmailCodeSender(passwordlessApiProperties, restTemplate);
    }

    /**
     * "Безопасно" проверяем параметр, не через константу
     */
    private static class ConsoleSenderCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            BindResult<PasswordlessApiProperties> bind = Binder.get(context.getEnvironment())
                    .bind(PasswordlessApiProperties.PREFIX, PasswordlessApiProperties.class);
            return bind.get().getLogCode();
        }
    }
}
