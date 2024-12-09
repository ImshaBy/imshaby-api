package by.imsha.domain.dto.mapper;

import by.imsha.domain.Parish;
import by.imsha.domain.dto.MassParishInfo;
import by.imsha.service.VolunteerService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import static by.imsha.utils.LocalizedUtils.getLocalizedParishAddress;
import static by.imsha.utils.LocalizedUtils.getLocalizedParishName;
import static by.imsha.utils.LocalizedUtils.getLocalizedParishShortName;
import static by.imsha.utils.UserLocaleHolder.getUserLocale;

/**
 * @author Alena Misan
 */
@Mapper(uses = LocationInfoMapper.class, componentModel = "spring")
public abstract class MassParishInfoMapper {

    @Autowired
    private VolunteerService volunteerService;

    @Mapping(source = "id", target = "parishId")
    @Mapping(source = "parish", target = "name", qualifiedByName = "convertToLocalizedName")
    @Mapping(source = "parish", target = "shortName", qualifiedByName = "convertToLocalizedShortName")
    @Mapping(source = "parish", target = "address", qualifiedByName = "convertToLocalizedAddress")
    @Mapping(source = "parish", target = "volunteerNeeded", qualifiedByName = "convertToVolunteerNeeded")
    public abstract MassParishInfo toMassParishInfo(Parish parish);

    @Named("convertToLocalizedName")
    public String convertToLocalizedName(final Parish parish) {
        return getLocalizedParishName(parish, getUserLocale().orElse(null));
    }

    @Named("convertToLocalizedShortName")
    public String convertToLocalizedShortName(final Parish parish) {
        return getLocalizedParishShortName(parish, getUserLocale().orElse(null));
    }

    @Named("convertToLocalizedAddress")
    public String convertToLocalizedAddress(final Parish parish) {
        return getLocalizedParishAddress(parish, getUserLocale().orElse(null));
    }

    @Named("convertToVolunteerNeeded")
    public Boolean convertToVolunteerNeeded(final Parish parish) {
        return volunteerService.volunteerNeededByParishName(parish.getName());
    }
}
