package by.imsha.mapper;

import by.imsha.meilisearch.model.SearchResultItem;
import by.imsha.meilisearch.reader.SearchResult;
import by.imsha.rest.dto.MassCoordinatesResponse;
import by.imsha.rest.dto.MassCoordinatesResponse.MassCoordinate;
import by.imsha.rest.dto.MassCoordinatesResponse.Parish;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MassCoordinatesMapper {

    //TODO всюду используется зона +3 (нужно хорошо всё обдумать и отрефакторить)
    ZoneOffset BEL_ZONE_OFFSET = ZoneOffset.ofHours(3);

    default MassCoordinatesResponse map(SearchResult searchResult, List<MassCoordinate> notApprovedAndNotDisabledParishesMassCoordinates) {
        List<MassCoordinate> massCoordinates = new ArrayList<>(notApprovedAndNotDisabledParishesMassCoordinates);

        massCoordinates.addAll(mapSearchResultItems(searchResult.hits()));

        return new MassCoordinatesResponse(massCoordinates);
    }

    List<MassCoordinate> mapSearchResultItems(List<SearchResultItem> searchResultItems);

    @Mapping(target = "massDateTime", source = "dateTime")
    @Mapping(target = "parish", source = ".")
    MassCoordinate map(SearchResultItem searchResultItem);

    @Mapping(target = "id", source = "parish.id")
    @Mapping(target = "geo", source = "geo")
    @Mapping(target = "actual", source = "parish.actual")
    @Mapping(target = "state", source = "parish.state")
    Parish mapToParish(SearchResultItem searchResultItem);

    default OffsetDateTime map(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }

        return OffsetDateTime.of(localDateTime, BEL_ZONE_OFFSET);
    }

    List<MassCoordinate> map(List<by.imsha.domain.Parish> parishes);

    @Mapping(target = "massDateTime", ignore = true)
    @Mapping(target = "parish", source = ".")
    MassCoordinate map(by.imsha.domain.Parish parish);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "actual", expression = "java(!parish.isNeedUpdate())")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "geo.lng", source = "gps.longitude")
    @Mapping(target = "geo.lat", source = "gps.latitude")
    Parish mapToParish(by.imsha.domain.Parish parish);
}
