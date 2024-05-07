package by.imsha.service;

import by.imsha.domain.Cors;
import by.imsha.repository.CorsConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class CorsConfigService {

    private final CorsConfigRepository corsConfigRepository;

    public Cors createCors(final @Valid Cors cors) {
        return corsConfigRepository.save(cors);
    }

    public Optional<Cors> getCors(String id) {
        return corsConfigRepository.findById(id);
    }

    public Page<Cors> getAllCors(Integer page, Integer size) {
        return corsConfigRepository.findAll(PageRequest.of(page, size));
    }

    public Cors updateCors(Cors cors) {
        return corsConfigRepository.save(cors);
    }
    @Caching(evict = {
            @CacheEvict(cacheNames = "corsConfiguration", allEntries = true)
    })
    public void removeCors(String id) {
        corsConfigRepository.deleteById(id);
    }

    @Cacheable("corsConfiguration")
    public Set<String> getLowerCaseOrigins() {
        return corsConfigRepository.findAll().stream()
                .map(cors -> cors.getOrigin().toLowerCase())
                .collect(Collectors.toSet());
    }
}