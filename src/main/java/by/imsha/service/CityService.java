package by.imsha.service;


import by.imsha.domain.City;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.repository.CityRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


@Service
public class CityService {

    private static final Logger log = LoggerFactory.getLogger(CityService.class);

    @Value("${imsha.city.name}")
    private String defaultCityName;

    public String getDefaultCityName() {
        return defaultCityName;
    }

    @Autowired
    private CityRepository cityRepository;


    @Autowired
    CounterService counterService;

    public City createCity(City city) {
        return cityRepository.save(city);
    }

    @Cacheable(cacheNames = "cityCache", key="'default'", unless = "#result != null")
    public City defaultCity(){
        return cityRepository.findByName(getDefaultCityName());
    }

    public String getCityIdOrDefault(String cityId) {
        if(StringUtils.isEmpty(cityId)){
            if(log.isWarnEnabled()){
                log.warn("Looking for default city..");
            }
            City defaultCity = defaultCity();
            if(defaultCity == null){
                throw new ResourceNotFoundException(String.format("No default city (name = %s) founded", getDefaultCityName()));
            }
            cityId = defaultCity.getId();
            if(log.isWarnEnabled()){
                log.warn(String.format("Default city with id = %s is found.", cityId));
            }
        }
        return cityId;
    }

    @CacheEvict(cacheNames = "cityCache", condition = "#result != null")
    public void removeCity(String id) {
        cityRepository.delete(id);
    }

    @CacheEvict(cacheNames = "cityCache", key = "#p0.id", condition = "#result != null")
    public City updateCity(City city) {
        return cityRepository.save(city);
    }


    @Cacheable(cacheNames = "cityCache")
    public City retrieveCity(String id){
        return cityRepository.findById(id);
    }

    public Page<City> getAllCities(Integer page, Integer size) {
        Page pageOfHotels = cityRepository.findAll(new PageRequest(page, size));
        // example of adding to the /metrics
        if (size > 50) {
            counterService.increment("by.imsha.service.CityService.getAllCities.largePayload");
        }
        return pageOfHotels;
    }

}
