package by.imsha.server.rest.filter;

import by.imsha.utils.UserLocaleHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.io.IOException;

/**
 * Фильтр, для заполнения локали пользователя в {@link UserLocaleHolder}
 */
@Component
public class UserLocaleManagingFilter extends OncePerRequestFilter {

    private static final String LANG_PARAM_NAME = "lang";

    private static String fetchUserLangFromHttpRequest(HttpServletRequest httpServletRequest) {
        String paramLang = httpServletRequest.getParameter(LANG_PARAM_NAME);
        if (StringUtils.isEmpty(paramLang)) {
            //it's ok to determine lang as part of locale as fallback user language
            paramLang = RequestContextUtils.getLocale(httpServletRequest).getLanguage();
        }
        return paramLang;
    }

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
}
