package by.imsha.domain.dto.mapper;

import by.imsha.domain.City;
import by.imsha.domain.dto.CityInfo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * @author Alena Misan
 */
@Mapper( nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS ,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = "spring")
public interface CityMapper {

    void updateCityFromDTO(CityInfo cityInfo, @MappingTarget City city);
}
