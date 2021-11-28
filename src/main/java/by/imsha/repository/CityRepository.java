package by.imsha.repository;


import by.imsha.domain.City;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;


public interface CityRepository extends QuerableMongoRepository<City, String> {

    @CachePut(cacheNames = "cityCache", key = "#result.id", condition = "#result != null")
    City findByName(String name);
    Optional<City> findById(String id);
}
