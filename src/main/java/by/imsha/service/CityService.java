package by.imsha.service;


import by.imsha.domain.City;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.properties.ImshaProperties;
import by.imsha.repository.CityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class CityService {

    private final ImshaProperties imshaProperties;
    private final CityRepository cityRepository;

    public String getDefaultCityName() {
        return imshaProperties.getDefaultCity().getName();
    }

    public City createCity(City city) {
        return cityRepository.save(city);
    }

    @Cacheable(cacheNames = "cityCache", key = "'default'", unless = "#result != null")
    public City defaultCity() {
        return cityRepository.findByName(getDefaultCityName());
    }

    public String getCityIdOrDefault(String cityId) {
        if (StringUtils.isEmpty(cityId)) {
            City defaultCity = defaultCity();

            if (defaultCity == null) {
                throw new ResourceNotFoundException(String.format("No default city (name = %s) founded", getDefaultCityName()));
            }

            cityId = defaultCity.getId();
        }
        return cityId;
    }

    @CacheEvict(cacheNames = "cityCache", condition = "#result != null")
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
}
