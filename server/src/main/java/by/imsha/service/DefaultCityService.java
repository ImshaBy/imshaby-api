package by.imsha.service;

import by.imsha.domain.City;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.repository.CityRepository;
import by.imsha.server.properties.ImshaProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultCityService {

    private final ImshaProperties imshaProperties;
    private final CityRepository cityRepository;

    public String getDefaultCityKey() {
        return imshaProperties.getDefaultCity().getKey();
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
}
