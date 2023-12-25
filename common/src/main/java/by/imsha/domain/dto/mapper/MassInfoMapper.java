package by.imsha.domain.dto.mapper;

import by.imsha.domain.Mass;
import by.imsha.domain.dto.MassInfo;
import by.imsha.domain.dto.UpdateMassInfo;
import by.imsha.utils.UserLocaleHolder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import static by.imsha.utils.LocalizedUtils.getLocalizedMassNotes;

/**
 * @author Alena Misan
 */
@Mapper(uses = ParishInfoMapper.class,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MassInfoMapper {
    MassInfoMapper MAPPER = Mappers.getMapper(MassInfoMapper.class);

    @Mapping(source = "mass", target = "info", qualifiedByName = "convertNotesToInfo")
    @Mapping(target = "parish", expression = "java(by.imsha.service.ParishService.extractMassParishInfo(mass.getParishId()))")
    MassInfo toMassInfo(Mass mass);

    @Named("convertNotesToInfo")
    default String convertNotesToInfo(final Mass mass) {
        return getLocalizedMassNotes(mass, UserLocaleHolder.getUserLocale().orElse(null));
    }

    Mass updateMassFromDTO(UpdateMassInfo massInfo, @MappingTarget Mass mass);
}
