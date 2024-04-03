package by.imsha.service;

import by.imsha.domain.Cors;
import by.imsha.repository.CorsConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorsConfigService {

    private final CorsConfigRepository corsConfigRepository;

    @Cacheable("corsConfiguration")
    public Set<String> getOriginsToLowerCase() {
        List<Cors> corsConfigs = corsConfigRepository.findAll();

        if (corsConfigs.isEmpty()) {
            Set<String> origin = new HashSet<>();
            origin.add("*");
            return origin;
        }
        return corsConfigs.stream()
                .map(cors -> cors.getOrigin().toLowerCase())
                .collect(Collectors.toSet());
    }
}