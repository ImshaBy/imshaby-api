package by.imsha.domain.dto.mapper;

import by.imsha.domain.Cors;
import by.imsha.domain.dto.CorsInfo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;


@Mapper( nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS ,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = "spring")
public interface CorsMapper {

    void updateCorsFromDTO(CorsInfo corsInfo, @MappingTarget Cors cors);
}