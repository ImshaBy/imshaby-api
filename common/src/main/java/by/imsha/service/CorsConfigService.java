package by.imsha.service;

import by.imsha.repository.CorsConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorsConfigService {

    private final CorsConfigRepository corsConfigRepository;

    @Cacheable("corsConfiguration")
    public Set<String> getLowerCaseOrigins() {
        return corsConfigRepository.findAll().stream()
                .map(cors -> cors.getOrigin().toLowerCase())
                .collect(Collectors.toSet());
    }
}