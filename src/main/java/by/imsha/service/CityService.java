package by.imsha.service;


import by.imsha.domain.City;
import by.imsha.domain.Parish;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.properties.ImshaProperties;
import by.imsha.repository.CityRepository;
import by.imsha.repository.ParishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class CityService {

    private final ImshaProperties imshaProperties;
    private final CityRepository cityRepository;
    private final ParishRepository parishRepository;

    public String getDefaultCityKey() {
        return imshaProperties.getDefaultCity().getKey();
    }

    public City createCity(City city) {
        return cityRepository.save(city);
    }

    @Cacheable(cacheNames = "cityCache", key = "'default'", unless = "#result != null")
    public City defaultCity() {
        return cityRepository.findByKey(getDefaultCityKey());
    }

    public String getCityIdOrDefault(String cityId) {
        if (StringUtils.isEmpty(cityId)) {
            City defaultCity = defaultCity();

            if (defaultCity == null) {
                throw new ResourceNotFoundException(String.format("No default city (name = %s) founded", getDefaultCityKey()));
            }

            cityId = defaultCity.getId();
        }
        return cityId;
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
