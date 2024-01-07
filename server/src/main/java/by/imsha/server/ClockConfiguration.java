package by.imsha.server;

import by.imsha.server.properties.ImshaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfiguration {

    /**
     * Часы, для работы со временем в системе
     */
    @Bean
    public Clock clock(final ImshaProperties imshaProperties) {
        return Clock.system(imshaProperties.getZoneId());
    }
}
