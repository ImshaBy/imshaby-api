package by.imsha.server;

import by.imsha.server.properties.ImshabyApiTestProperties;
import by.imsha.utils.DateTimeProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Configuration
@EnableConfigurationProperties(ImshabyApiTestProperties.class)
public class TestConfiguration {

    public static final String ERROR_TIMESTAMP = "2023-07-16T16:43:01+03:00";

    @Bean
    public DateTimeProvider dateTimeProvider() {
        return new DateTimeProvider(Clock.fixed(ZonedDateTime.parse(ERROR_TIMESTAMP).toInstant(), ZoneId.of("Europe/Minsk")));
    }
}
