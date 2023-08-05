package by.imsha.repository;

import by.imsha.domain.Parish;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.Optional;

/**
 *
 */
public interface ParishRepository extends QuerableMongoRepository<Parish, String>{


    @Caching(evict = {
            @CacheEvict(cacheNames = "parishCache", key = "#p0.id"),
            @CacheEvict(cacheNames = "pendingParishes", key = "'parishCity:' + #p0.cityId")
    })
    Parish save(Parish parish);


    @Caching(evict = {
            @CacheEvict(cacheNames = "parishCache"),
            @CacheEvict(cacheNames = "pendingParishes", allEntries = true)
    })
    void deleteParishById(String id);


    @Cacheable(cacheNames = "parishCache")
    Optional<Parish> findById(String id);

    Parish findByUserId(String userId);
}
