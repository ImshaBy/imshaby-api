package by.imsha.mapper;

import by.imsha.meilisearch.model.SearchResultItem;
import by.imsha.meilisearch.reader.SearchResult;
import by.imsha.rest.dto.MassCoordinatesResponse;
import by.imsha.rest.dto.MassCoordinatesResponse.MassCoordinate;
import by.imsha.rest.dto.MassCoordinatesResponse.Parish;
import by.imsha.rest.dto.MassCoordinatesResponse.UnapprovedParish;
import by.imsha.service.CityService;
import by.imsha.service.VolunteerService;
import by.imsha.utils.LocalizedUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static by.imsha.utils.LocalizedUtils.getLocalizedParishAddress;
import static by.imsha.utils.LocalizedUtils.getLocalizedParishName;

@Mapper(componentModel = "spring")
public abstract class MassCoordinatesMapper {

    //TODO всюду используется зона +3 (нужно хорошо всё обдумать и отрефакторить)
    protected static final ZoneOffset BEL_ZONE_OFFSET = ZoneOffset.ofHours(3);

    @Autowired
    private VolunteerService volunteerService;

    @Autowired
    private CityService cityService;

    public MassCoordinatesResponse map(SearchResult searchResult,
                                       List<UnapprovedParish> unapprovedParishes) {

        return new MassCoordinatesResponse(
                mapSearchResultItems(searchResult.hits()),
                unapprovedParishes
        );
    }

    public List<UnapprovedParish> mapUnapprovedParishes(List<by.imsha.domain.Parish> parishes, String locale) {
        return parishes.stream().map(parish -> mapToUnapprovedParish(parish, locale)).toList();
    }

    private UnapprovedParish mapToUnapprovedParish(by.imsha.domain.Parish parish, String locale) {
        String cityName = cityService.retrieveCity(parish.getCityId())
                .map(city -> LocalizedUtils.getLocalizedCityName(city, locale))
                .orElse(null);
        return new UnapprovedParish(getLocalizedParishAddress(parish, locale), cityName, parish.getImgPath(),
                getLocalizedParishName(parish, locale), parish.getId(),
                volunteerService.volunteerNeededByParishName(parish.getKey(), true));
    }

    public abstract List<MassCoordinate> mapSearchResultItems(List<SearchResultItem> searchResultItems);

    @Mapping(target = "massDateTime", source = "dateTime")
    @Mapping(target = "parish", source = ".")
    public abstract MassCoordinate map(SearchResultItem searchResultItem);

    @Mapping(target = "id", source = "parish.id")
    @Mapping(target = "geo", source = "geo")
    @Mapping(target = "actual", source = "parish.actual")
    @Mapping(target = "state", source = "parish.state")
    public abstract Parish mapToParish(SearchResultItem searchResultItem);

    public OffsetDateTime map(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }

        return OffsetDateTime.of(localDateTime, BEL_ZONE_OFFSET);
    }

    public abstract List<MassCoordinate> map(List<by.imsha.domain.Parish> parishes);

    @Mapping(target = "massDateTime", ignore = true)
    @Mapping(target = "parish", source = ".")
    public abstract MassCoordinate map(by.imsha.domain.Parish parish);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "actual", expression = "java(!parish.isNeedUpdate())")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "geo.lng", source = "gps.longitude")
    @Mapping(target = "geo.lat", source = "gps.latitude")
    public abstract Parish mapToParish(by.imsha.domain.Parish parish);
}
