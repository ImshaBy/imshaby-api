package by.imsha.security;

import by.imsha.properties.ImshaProperties;
import by.imsha.security.filter.ApiKeyAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * @author Alena Misan
 */

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private static final String PRODUCTION_PROFILE = "prod";
    private static final String QA_PROFILE = "qa";
    private static final String LOCAL_PROFILE = "local";

    /**
     * Включаем работу с аннотациями только для профилей prod и qa
     */
    @Configuration
    @Profile(value = {PRODUCTION_PROFILE, QA_PROFILE, LOCAL_PROFILE})
    @EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
    public static class SecurityAnnotationsEnableConfiguration {
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsConfiguration corsConfiguration) {
        //конфигурация, для разрешения запроса /api/mass/parish-week с любыми origin
        //по-умолчанию allowCredentials = false, т.к. с '*' это единственный вариант
        //если нужно будет allowCredentials = true, то нужно костылить в source получения конфига и
        //подбрасывать в allowedOrigins значение origin из текущего запроса
        CorsConfiguration allowAnyRequestCorsConfiguration = new CorsConfiguration();
        allowAnyRequestCorsConfiguration.setAllowedOrigins(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/mass/parish-week", allowAnyRequestCorsConfiguration);
        source.registerCorsConfiguration("/**", corsConfiguration);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment,
                                                   ImshaProperties imshaProperties) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        //защищаем только на prod и qa
        if (environment.acceptsProfiles(Profiles.of(PRODUCTION_PROFILE, QA_PROFILE, LOCAL_PROFILE))) {
            http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                            //ping controller
                            .requestMatchers(HttpMethod.GET, "/").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/passwordless/start").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/passwordless/login").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/mass/week").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/parish/*/state").permitAll()
                            .requestMatchers(HttpMethod.GET, "/hook/parish").permitAll()
                            .requestMatchers(HttpMethod.GET, "/hook/mass").permitAll()
                            .requestMatchers(HttpMethod.GET, "/status").permitAll()
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                            .anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(oauth2ResourceServer ->
                            oauth2ResourceServer.jwt(Customizer.withDefaults())
                    );


            //при наличии API ключей добавляем фильтр для работы с ними
            if (!CollectionUtils.isEmpty(imshaProperties.getApiKeys()) ||
                    !CollectionUtils.isEmpty(imshaProperties.getInternalApiKeys())) {
                http.addFilterBefore(
                        new ApiKeyAuthenticationFilter(
                                Collections.unmodifiableSet(
                                        new HashSet<>(imshaProperties.getApiKeys())
                                ),
                                Collections.unmodifiableSet(
                                        new HashSet<>(imshaProperties.getInternalApiKeys())
                                )
                        ),
                        BearerTokenAuthenticationFilter.class
                );
                log.info("API-key authentication filter enabled!");
            } else {
                log.info("API-key authentication filter disabled!");
            }
        }

        return http.build();
    }

}
