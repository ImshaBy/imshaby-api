package by.imsha.service;

import by.imsha.domain.Cors;
import by.imsha.repository.CorsConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorsConfigServiceTest {

    @InjectMocks
    CorsConfigService corsConfigService;

    @Mock
    CorsConfigRepository corsConfigRepository;

    @Test
    void testGetOriginsSuccess() {
        Cors origin = Cors.builder()
                .origin("http://IMSHA.by")
                .build();
        when(corsConfigRepository.findAll()).thenReturn(Collections.singletonList(origin));

        Set<String> originsToLowerCase = corsConfigService.getLowerCaseOrigins();

        assertAll(
                () -> verify(corsConfigRepository).findAll(),
                () -> assertThat(originsToLowerCase.contains("http://imsha.by")).isTrue()
        );
    }
}