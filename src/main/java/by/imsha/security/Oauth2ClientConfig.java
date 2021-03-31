package by.imsha.security;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Configuration//@EnableWebFluxSecurity
public class Oauth2ClientConfig {


   /* @Bean
    public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository(OAuth2ClientProperties oAuth2ClientProperties) {
        List<ClientRegistration> clientRegistrations = new ArrayList<>();

        // because autoconfigure does not work for an unknown reason, here the ClientRegistrations are manually configured based on the application.yml
        oAuth2ClientProperties.getRegistration()
                .forEach((k, v) -> {
                    String tokenUri = oAuth2ClientProperties.getProvider().get(k).getTokenUri();
                    ClientRegistration clientRegistration = ClientRegistration
                            .withRegistrationId(k)
                            .tokenUri(tokenUri)
                            .clientId(v.getClientId())
                            .clientSecret(v.getClientSecret())
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                            .build();
                    clientRegistrations.add(clientRegistration);
                });

        return new InMemoryReactiveClientRegistrationRepository(clientRegistrations);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    public ServerOAuth2AuthorizedClientRepository credHubReactiveAuthorizedClientRepository() {
        return new UnAuthenticatedServerOAuth2AuthorizedClientRepository();
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(final ReactiveClientRegistrationRepository clientRegistrationRepository,
                                                                         final ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
        ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        DefaultReactiveOAuth2AuthorizedClientManager authorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
        return authorizedClientManager;
    }

    @Bean(name = "aaron")
    public WebClient webClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth.setDefaultClientRegistrationId("aaron");
        return WebClient.builder().filter(oauth).build();
    }
*/
//    @Bean
//    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
//        return http.oauth2Client().and().build();
//    }
}
