package by.imsha;

import by.imsha.properties.CommonProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfiguration {

    /**
     * Часы, для работы со временем в системе
     */
    @Bean
    public Clock clock(final CommonProperties imshaProperties) {
        return Clock.system(imshaProperties.getZoneId());
    }
}
