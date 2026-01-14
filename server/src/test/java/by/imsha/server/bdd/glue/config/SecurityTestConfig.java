package by.imsha.server.bdd.glue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityTestConfig {

    /**
     * В тестах заполняется Bearer parishKey
     * <p>
     * поэтому парсить токен нет необходимости, только устанавливаем claim parishes значением токена
     */
    @Bean
    JwtDecoder jwtDecoder() {
        return parishKey -> {
            Instant now = Instant.now();
            Map<String, Object> claims = new HashMap<>();
            claims.put("parishes", List.of(parishKey));
            return new Jwt("FAKE", now.minusSeconds(10), now.plusSeconds(300), Map.of("fake", "fake"), claims);
        };
    }
}
