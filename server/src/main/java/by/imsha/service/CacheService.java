package by.imsha.service;

import lombok.AllArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@AllArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;

    public void clearAllCache() {
        cacheManager.getCacheNames().parallelStream()
                .forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }
}
