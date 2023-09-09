package by.imsha.repository;


import by.imsha.domain.City;
import org.springframework.cache.annotation.CachePut;

import java.util.Optional;


public interface CityRepository extends QuerableMongoRepository<City, String> {

    @CachePut(cacheNames = "cityCache", key = "#result.id", condition = "#result != null")
    City findByName(String name);

    @CachePut(cacheNames = "cityCache", key = "#result.id", condition = "#result != null")
    City findByKey(String key);
    Optional<City> findById(String id);
}
