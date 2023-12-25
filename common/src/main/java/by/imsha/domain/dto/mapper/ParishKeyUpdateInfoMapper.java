package by.imsha.domain.dto.mapper;

import by.imsha.domain.Parish;
import by.imsha.domain.dto.ParishKeyUpdateInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import static by.imsha.utils.LocalizedUtils.getLocalizedParishName;
import static by.imsha.utils.UserLocaleHolder.getUserLocale;

/**
 * @author Alena Misan
 */
@Mapper
public interface ParishKeyUpdateInfoMapper {
    ParishKeyUpdateInfoMapper MAPPER = Mappers.getMapper(ParishKeyUpdateInfoMapper.class);

    @Mapping(source = "parish", target = "name", qualifiedByName = "convertToLocalizedName")
    ParishKeyUpdateInfo toParishKeyUpdateInfo(Parish parish);

    @Named("convertToLocalizedName")
    default String convertToLocalizedName(final Parish parish) {
        return getLocalizedParishName(parish, getUserLocale().orElse(null));
    }
}
