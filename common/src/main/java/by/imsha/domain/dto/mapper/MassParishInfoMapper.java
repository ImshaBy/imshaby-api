package by.imsha.domain.dto.mapper;

import by.imsha.domain.Parish;
import by.imsha.domain.dto.MassParishInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import static by.imsha.utils.LocalizedUtils.getLocalizedParishAddress;
import static by.imsha.utils.LocalizedUtils.getLocalizedParishName;
import static by.imsha.utils.LocalizedUtils.getLocalizedParishShortName;
import static by.imsha.utils.UserLocaleHolder.getUserLocale;

/**
 * @author Alena Misan
 */
@Mapper(uses = {LocationInfoMapper.class})
public interface MassParishInfoMapper {
    MassParishInfoMapper MAPPER = Mappers.getMapper(MassParishInfoMapper.class);

    @Mapping(source = "id", target = "parishId")
    @Mapping(source = "parish", target = "name", qualifiedByName = "convertToLocalizedName")
    @Mapping(source = "parish", target = "shortName", qualifiedByName = "convertToLocalizedShortName")
    @Mapping(source = "parish", target = "address", qualifiedByName = "convertToLocalizedAddress")
    MassParishInfo toMassParishInfo(Parish parish);

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
