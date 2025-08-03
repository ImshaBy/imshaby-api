package by.imsha.rest.passwordless;

import by.imsha.properties.PasswordlessApiProperties;
import by.imsha.rest.passwordless.mapper.FusionauthMapper;
import by.imsha.rest.passwordless.send.CodeSender;
import by.imsha.rest.passwordless.send.ConsoleCodeSender;
import by.imsha.rest.passwordless.send.EmailCodeSender;
import api_specification.by.imsha.server.fusionauth.public_client.api.FusionauthPublicApiClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
public class PasswordlessConfig {

    @Bean
    @Conditional(ConsoleSenderCondition.class)
    public CodeSender consoleCodeSender() {
        return new ConsoleCodeSender();
    }

    @Bean
    @ConditionalOnMissingBean
    public CodeSender codeSender(FusionauthPublicApiClient fusionauthPublicApiClient,
                                 FusionauthMapper fusionauthMapper) {
        return new EmailCodeSender(fusionauthPublicApiClient, fusionauthMapper);
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
