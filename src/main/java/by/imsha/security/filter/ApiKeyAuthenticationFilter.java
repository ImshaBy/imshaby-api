package by.imsha.security.filter;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Фильтр аутентификации с помощью API-ключа (заголовок {@link ApiKeyAuthenticationFilter#API_KEY_HEADER_NAME})
 */
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends GenericFilterBean {

    private static final String API_KEY_HEADER_NAME = "X-Api-Key";

    /**
     * api-ключи позволяющие получить доступ к бизнес-функциям
     */
    private final Set<String> apiKeys;
    /**
     * api-ключи, позволяющие получить доступ к приватным функциям
     */
    private final Set<String> internalApiKeys;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        try {

            Optional<String> header = Optional.ofNullable(((HttpServletRequest) request).getHeader(API_KEY_HEADER_NAME));

            //Пытаемся получить аутентификацию по internal API-key
            Optional<ApiKeyAuthentication> apiKeyAuthentication = header
                    .filter(internalApiKeys::contains)
                    .map(apiKey -> new ApiKeyAuthentication(apiKey, AuthorityUtils.createAuthorityList("ROLE_INTERNAL")));

            //Пытаемся получить аутентификацию по API-key, если не был использован internal API-key
            if (!apiKeyAuthentication.isPresent()) {
                apiKeyAuthentication = header
                        .filter(apiKeys::contains)
                        .map(apiKey -> new ApiKeyAuthentication(apiKey, AuthorityUtils.NO_AUTHORITIES));
            }

            apiKeyAuthentication
                    .ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
        } catch (Exception exception) {
            //в случае ошибки не прерываем фильтрацию
            log.error("Api-Key authentication error!", exception);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Класс аутентификации по API-key
     */
    @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
    public static class ApiKeyAuthentication extends AbstractAuthenticationToken {

        @EqualsAndHashCode.Include
        private final String apiKey;

        public ApiKeyAuthentication(String apiKey, Collection<? extends GrantedAuthority> authorities) {
            super(authorities);
            this.apiKey = apiKey;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return apiKey;
        }
    }
}
