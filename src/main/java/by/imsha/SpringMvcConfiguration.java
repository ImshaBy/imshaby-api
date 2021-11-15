package by.imsha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.util.Locale;

@EnableAutoConfiguration
@Configuration
public class SpringMvcConfiguration {


    @Value("${imsha.lang.cookie}")
    private String langCookieName;

    @Value("${spring.mvc.locale}")
    private Locale locale;

    @Bean
    public LocaleResolver localeResolver(){
        CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
        cookieLocaleResolver.setCookieName(langCookieName);
        cookieLocaleResolver.setDefaultLocale(locale);
        return cookieLocaleResolver;
    }
}
