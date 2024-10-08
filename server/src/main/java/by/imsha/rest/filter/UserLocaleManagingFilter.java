package by.imsha.rest.filter;

import by.imsha.utils.UserLocaleHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.Locale;

/**
 * Фильтр, для заполнения локали пользователя в {@link UserLocaleHolder}
 *
 * TODO переделать работу с локалями в целом (нужно выбрать какой-то конкретный способ конфигурации)
 */
@Component
@Deprecated(forRemoval = true)
@RequiredArgsConstructor
public class UserLocaleManagingFilter extends OncePerRequestFilter {

    private static final String LANG_PARAM_NAME = "lang";

    private final LocaleResolver localeResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        UserLocaleHolder.setUserLocale(fetchUserLangFromHttpRequest(request));

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserLocaleHolder.resetUserLocale();
        }
    }

    private String fetchUserLangFromHttpRequest(HttpServletRequest httpServletRequest) {
        String paramLang = httpServletRequest.getParameter(LANG_PARAM_NAME);
        if (StringUtils.isEmpty(paramLang)) {
            Locale locale = localeResolver.resolveLocale(httpServletRequest);
            if (locale == null) {
                locale = httpServletRequest.getLocale();
            }
            paramLang = locale.getLanguage();
        }
        return paramLang;
    }
}
