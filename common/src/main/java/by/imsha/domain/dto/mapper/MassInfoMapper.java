package by.imsha.domain.dto.mapper;

import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.MassInfo;
import by.imsha.domain.dto.MassParishInfo;
import by.imsha.domain.dto.UpdateMassInfo;
import by.imsha.service.ParishService;
import by.imsha.utils.UserLocaleHolder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import static by.imsha.utils.LocalizedUtils.getLocalizedMassNotes;

/**
 * @author Alena Misan
 */
@Mapper(uses = ParishInfoMapper.class,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = "spring")
public abstract class MassInfoMapper {

    @Autowired
    ParishService parishService;

    @Autowired
    MassParishInfoMapper massParishInfoMapper;

    @Mapping(source = "mass", target = "info", qualifiedByName = "convertNotesToInfo")
    @Mapping(source = "mass", target = "parish", qualifiedByName = "extractMassParishInfo")
    public abstract MassInfo toMassInfo(Mass mass);

    @Named("convertNotesToInfo")
    public String convertNotesToInfo(final Mass mass) {
        return getLocalizedMassNotes(mass, UserLocaleHolder.getUserLocale().orElse(null));
    }

    public abstract Mass updateMassFromDTO(UpdateMassInfo massInfo, @MappingTarget Mass mass);

    @Named("extractMassParishInfo")
    public MassParishInfo extractMassParishInfo(Mass mass){
        Parish parish = parishService.getParish(mass.getParishId()).get();
        return massParishInfoMapper.toMassParishInfo(parish);
    }
}
