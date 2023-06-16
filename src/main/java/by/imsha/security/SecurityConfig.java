package by.imsha.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * @author Alena Misan
 */

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsConfiguration corsConfiguration) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment) throws Exception {
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

                    // temporary before integration BOT with Auth server
                    .antMatchers(HttpMethod.GET, "/api/parish/*").permitAll()
                    .antMatchers(HttpMethod.GET, "/api/parish").permitAll()
                    .antMatchers(HttpMethod.GET, "/api/mass/*").permitAll()
                    .antMatchers(HttpMethod.PUT, "/api/mass").permitAll()
                    .anyRequest().authenticated();
        }

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.oauth2ResourceServer().jwt();

        return http.build();
    }

}
