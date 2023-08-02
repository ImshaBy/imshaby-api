package by.imsha.security;

import by.imsha.properties.ImshaProperties;
import by.imsha.security.filter.ApiKeyAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.HashSet;

/**
 * @author Alena Misan
 */

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsConfiguration corsConfiguration) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment,
                                                   ImshaProperties imshaProperties) throws Exception {
        http.cors()
                .and()
                .csrf().disable();

        //защищаем только на prod и qa
        if (environment.acceptsProfiles(Profiles.of("prod", "qa"))) {
            http.authorizeRequests()
                    //ping controller
                    .antMatchers(HttpMethod.GET, "/").permitAll()

                    .antMatchers(HttpMethod.POST, "/api/passwordless/start").permitAll()
                    .antMatchers(HttpMethod.POST, "/api/passwordless/login").permitAll()
                    .antMatchers(HttpMethod.GET, "/api/mass/week").permitAll()
                    .antMatchers(HttpMethod.GET, "/hook/parish").permitAll()
                    .antMatchers(HttpMethod.GET, "/hook/mass").permitAll()
                    .antMatchers(HttpMethod.GET, "/status").permitAll()
                    .anyRequest().authenticated();

            //при наличии API ключей добавляем фильтр для работы с ними
            if (!CollectionUtils.isEmpty(imshaProperties.getApiKeys())) {
                http.addFilterBefore(
                        new ApiKeyAuthenticationFilter(
                                Collections.unmodifiableSet(
                                        new HashSet<>(imshaProperties.getApiKeys())
                                )
                        ),
                        BearerTokenAuthenticationFilter.class
                );
                log.info("API-key authentication filter enabled!");
            } else {
                log.info("API-key authentication filter disabled!");
            }

            http.oauth2ResourceServer().jwt();
        }

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http.build();
    }

}
