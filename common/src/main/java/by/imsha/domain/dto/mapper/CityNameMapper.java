package by.imsha.domain.dto.mapper;

import by.imsha.domain.City;
import by.imsha.domain.dto.CityName;
import by.imsha.service.CityService;
import by.imsha.utils.UserLocaleHolder;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static by.imsha.utils.LocalizedUtils.getLocalizedCityName;

@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = "spring")
public class CityNameMapper {

    @Autowired
    private CityService cityService;

    public CityName mapToCityName(City city) {
        return Optional.ofNullable(getLocalizedCityName(city, UserLocaleHolder.getUserLocale().orElse(null)))
                .map(cityName -> CityName.builder()
                        .name(cityName)
                        .build()
                )
                .orElse(null);
    }
}
