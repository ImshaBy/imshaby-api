package by.imsha.repository;


import by.imsha.domain.City;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CityRepository extends MongoRepository<City, String> {
    public City findByName(String name);
}
