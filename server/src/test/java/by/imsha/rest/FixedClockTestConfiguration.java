package by.imsha.rest;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
class FixedClockTestConfiguration {
    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(Instant.parse("2023-07-16T13:43:01Z"), ZoneId.of("Europe/Minsk"));
    }
}
