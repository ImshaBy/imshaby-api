package by.imsha.repository;

import by.imsha.domain.Parish;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import java.util.Optional;

/**
 *
 */
public interface ParishRepository extends QuerableMongoRepository<Parish, String>{


    @CacheEvict(cacheNames = "parishCache", key = "#p0.id")
    Parish save(Parish parish);


    @CacheEvict(cacheNames = "parishCache")
    void deleteParishById(String id);


    @Cacheable(cacheNames = "parishCache")
    Optional<Parish> findById(String id);

    Parish findByUserId(String userId);
}
