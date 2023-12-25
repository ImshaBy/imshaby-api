package by.imsha.service;


import by.imsha.domain.City;
import by.imsha.domain.Parish;
import by.imsha.repository.CityRepository;
import by.imsha.repository.ParishRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
@Validated
public class CityService {

    private final CityRepository cityRepository;
    private final ParishRepository parishRepository;

    public City createCity(final @Valid City city) {
        return cityRepository.save(city);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "cityCache", condition = "#result != null"),
            @CacheEvict(cacheNames = "pendingParishes", key = "'parishCity:' + #p0"),
            @CacheEvict(cacheNames = "citiesWithParishCache", allEntries = true)
    })
    public void removeCity(String id) {
        cityRepository.deleteById(id);
    }

    @CacheEvict(cacheNames = "cityCache", key = "#p0.id", condition = "#result != null")
    public City updateCity(City city) {
        return cityRepository.save(city);
    }


    @Cacheable(cacheNames = "cityCache")
    public Optional<City> retrieveCity(String id) {
        return cityRepository.findById(id);
    }

    public Page<City> getAllCities(Integer page, Integer size) {
        return cityRepository.findAll(PageRequest.of(page, size));
    }

    @Cacheable(cacheNames = "cityCache", key = "'allCities'", unless = "#result != null")
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    @Cacheable(cacheNames = "citiesWithParishCache")
    public Set<String> getCityWithApprovedParishesIds() {
        return parishRepository.findByState(Parish.State.APPROVED).stream()
                .map(Parish::getCityId)
                .collect(Collectors.toSet());
    }
}
