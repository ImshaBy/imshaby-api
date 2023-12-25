package by.imsha.domain.dto.mapper;

import by.imsha.domain.LocalizedParish;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.ParishInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import static by.imsha.utils.LocalizedUtils.getLocalizedParishAddress;
import static by.imsha.utils.LocalizedUtils.getLocalizedParishName;
import static by.imsha.utils.LocalizedUtils.getLocalizedParishShortName;
import static by.imsha.utils.UserLocaleHolder.getUserLocale;

/**
 * @author Alena Misan
 */
@Mapper(uses = {LocationInfoMapper.class},
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ParishInfoMapper {
    ParishInfoMapper MAPPER = Mappers.getMapper(ParishInfoMapper.class);

    @Mapping(source = "parish", target = "name", qualifiedByName = "convertToLocalizedName")
    @Mapping(source = "parish", target = "shortName", qualifiedByName = "convertToLocalizedShortName")
    @Mapping(source = "parish", target = "address", qualifiedByName = "convertToLocalizedAddress")
    ParishInfo toParishInfo(Parish parish);

    Parish updateParishFromDTO(ParishInfo parishInfo, @MappingTarget Parish parish);

    LocalizedParish updateLocalizedParishFromDTO(LocalizedParish parishInfo, @MappingTarget LocalizedParish parish);

    @Named("convertToLocalizedName")
    default String convertToLocalizedName(final Parish parish) {
        return getLocalizedParishName(parish, getUserLocale().orElse(null));
    }

    @Named("convertToLocalizedShortName")
    default String convertToLocalizedShortName(final Parish parish) {
        return getLocalizedParishShortName(parish, getUserLocale().orElse(null));
    }

    @Named("convertToLocalizedAddress")
    default String convertToLocalizedAddress(final Parish parish) {
        return getLocalizedParishAddress(parish, getUserLocale().orElse(null));
    }
}
