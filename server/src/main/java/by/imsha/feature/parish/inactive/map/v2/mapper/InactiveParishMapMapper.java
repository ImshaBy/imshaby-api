package by.imsha.feature.parish.inactive.map.v2.mapper;

import api_specification.by.imsha.server.inactive_parish_map.v2.server.model.InactiveParishMapGeo;
import api_specification.by.imsha.server.inactive_parish_map.v2.server.model.InactiveParishMapItem;
import by.imsha.domain.Coordinate;
import by.imsha.domain.Parish;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapping;

import static by.imsha.feature.parish.inactive.map.v2.FeatureMetadata.COMPONENT_VERSION;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        implementationName = "inactiveParishMapMapper" + COMPONENT_VERSION)
public interface InactiveParishMapMapper {
    @Mapping(target = "name", expression = "java(by.imsha.utils.LocalizedUtils.getLocalizedParishName(parish, locale))")
    @Mapping(target = "state", expression = "java(parish.getState().name())")
    @Mapping(target = "geo", source = "parish.gps")
    InactiveParishMapItem map(Parish parish, String locale);

    @Mapping(target = "lat", source = "latitude")
    @Mapping(target = "lng", source = "longitude")
    InactiveParishMapGeo map(Coordinate coordinate);
}
