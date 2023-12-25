package by.imsha.config;

import by.imsha.properties.config.DynamicCorsConfiguration;
import by.imsha.service.CorsConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicCorsConfigurationTest {

    @InjectMocks
    DynamicCorsConfiguration dynamicCorsConfiguration;

    @Mock
    CorsConfigService corsConfigService;

    @Test
    void testCheckOriginSuccess() {
        String requestOrigin = "http://IMSHA.by";
        Set<String> origins = new HashSet<>();
        origins.add("http://imsha.by");

        when(corsConfigService.getLowerCaseOrigins()).thenReturn(origins);

        String checkOrigin = dynamicCorsConfiguration.checkOrigin(requestOrigin);

        assertAll(
                () -> verify(corsConfigService).getLowerCaseOrigins(),
                () -> assertThat(checkOrigin).isEqualTo(requestOrigin)
        );
    }

    @Test
    void testCheckOriginWithAll() {
        String requestOrigin = "http://IMSHA.by";
        Set<String> origins = new HashSet<>();
        origins.add("http://imsha.by");
        origins.add("*");

        when(corsConfigService.getLowerCaseOrigins()).thenReturn(origins);

        String checkOrigin = dynamicCorsConfiguration.checkOrigin(requestOrigin);

        assertAll(
                () -> verify(corsConfigService).getLowerCaseOrigins(),
                () -> assertThat(checkOrigin).isEqualTo("*")
        );
    }

    @Test
    void testCheckOriginWithOriginsEmpty() {
        String requestOrigin = "http://IMSHA.by";
        dynamicCorsConfiguration.setAllowedOrigins(Collections.singletonList("http://IMsha.by"));

        when(corsConfigService.getLowerCaseOrigins()).thenReturn(new HashSet<>());

        String checkOrigin = dynamicCorsConfiguration.checkOrigin(requestOrigin);

        assertAll(
                () -> verify(corsConfigService).getLowerCaseOrigins(),
                () -> assertThat(checkOrigin).isEqualTo(requestOrigin)
        );
    }

    @Test
    void testCheckOriginWithAllowedOriginsEmpty() {
        String requestOrigin = "http://IMSHA.by";
        dynamicCorsConfiguration.setAllowedOrigins(Collections.emptyList());

        when(corsConfigService.getLowerCaseOrigins()).thenReturn(new HashSet<>());

        String checkOrigin = dynamicCorsConfiguration.checkOrigin(requestOrigin);

        assertAll(
                () -> verify(corsConfigService).getLowerCaseOrigins(),
                () -> assertThat(checkOrigin).isNull()
        );
    }

    @Test
    void testCheckOriginWithRequestOriginIsEmpty() {
        String requestOrigin = "";

        String checkOrigin = dynamicCorsConfiguration.checkOrigin(requestOrigin);

        assertAll(
                () -> verify(corsConfigService, times(0)).getLowerCaseOrigins(),
                () -> assertThat(checkOrigin).isNull()
        );
    }

    @Test
    void testCheckOriginWithRequestOriginIsNull() {
        String requestOrigin = null;

        String checkOrigin = dynamicCorsConfiguration.checkOrigin(requestOrigin);

        assertAll(
                () -> verify(corsConfigService, times(0)).getLowerCaseOrigins(),
                () -> assertThat(checkOrigin).isNull()
        );
    }
}