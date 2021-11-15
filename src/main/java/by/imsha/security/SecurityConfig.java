package by.imsha.security;

import com.auth0.spring.security.api.JwtWebSecurityConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * @author Alena Misan
 */

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {


    @Value("${spring.profiles.active}")
    private String env;

    @Value(value = "${auth0.audience}")
    private String apiAudience;

    @Value(value = "${auth0.issuer}")
    private String issuer;

    @Value(value = "${auth0.secret}")
    private String secret;


    @Value(value = "${cors.urls}")
    private String[] urls;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(urls));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("*"));
//        configuration.addAllowedHeader("Content-Type");

//        configuration.addExposedHeader("Content-Type");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        authorizeRequests(http);
        JwtWebSecurityConfigurer
                .forRS256(apiAudience, issuer)
                .configure(http);
    }


    /**
     * Our API Configuration - for Profile CRUD operations
     * <p>
     * Here we choose not to bother using the `auth0.securedRoute` property configuration
     * and instead ensure any unlisted endpoint in our config is secured by default
     */
    protected void authorizeRequests(final HttpSecurity http) throws Exception {
        http.cors()
                .and()
                .csrf().disable();

        if (env.equals("prod")) {
//        http.cors();

        http.authorizeRequests()
                .antMatchers(HttpMethod.GET, "/api/mass/week").permitAll()
                .antMatchers(HttpMethod.GET, "/hook/parish").permitAll()
                .antMatchers(HttpMethod.GET, "/hook/mass").permitAll()
                .anyRequest().authenticated();
         }
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//        }

    }

}
