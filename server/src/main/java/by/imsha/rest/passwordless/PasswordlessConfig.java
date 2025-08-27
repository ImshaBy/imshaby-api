package by.imsha.rest.passwordless;

import api_specification.by.imsha.common.fusionauth.public_client.api.FusionauthPublicApiClient;
import by.imsha.rest.passwordless.mapper.FusionauthMapper;
import by.imsha.rest.passwordless.send.CodeSender;
import by.imsha.rest.passwordless.send.EmailCodeSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordlessConfig {

    @Bean
    @ConditionalOnMissingBean
    public CodeSender codeSender(FusionauthPublicApiClient fusionauthPublicApiClient,
                                 FusionauthMapper fusionauthMapper) {
        return new EmailCodeSender(fusionauthPublicApiClient, fusionauthMapper);
    }
}
