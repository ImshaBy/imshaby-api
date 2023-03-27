package by.imsha;

import by.imsha.properties.ImshaProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

@Configuration
public class SpringMvcConfiguration {

    @Bean
    public LocaleResolver localeResolver(WebProperties webProperties, ImshaProperties imshaProperties) {
        CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
        cookieLocaleResolver.setCookieName(imshaProperties.getLangCookie());
        cookieLocaleResolver.setDefaultLocale(webProperties.getLocale());
        return cookieLocaleResolver;
    }
}
