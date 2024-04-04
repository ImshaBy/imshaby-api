package by.imsha.properties.config;

import by.imsha.service.CorsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Set;

public class DynamicCorsConfiguration extends CorsConfiguration {
    @Autowired
    private CorsConfigService corsConfigService;

    @Override
    public String checkOrigin(String requestOrigin) {
        if (!StringUtils.hasText(requestOrigin)) {
            return null;
        }
        String lowerCaseRequestOrigin = requestOrigin.toLowerCase();
        Set<String> allowedOrigins = corsConfigService.getLowerCaseOrigins();
        if (!ObjectUtils.isEmpty(allowedOrigins)) {
            if (allowedOrigins.contains(ALL)) {
                validateAllowCredentials();
                return ALL;
            }
            if (allowedOrigins.contains(lowerCaseRequestOrigin)) {
                return requestOrigin;
            }
        }
        return null;
    }
}