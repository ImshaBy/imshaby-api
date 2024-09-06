package by.imsha.domain.dto.mapper;

import by.imsha.domain.Coordinate;
import by.imsha.domain.dto.LocationInfo;
import org.mapstruct.Mapper;

/**
 * @author Alena Misan
 */
@Mapper(componentModel = "spring")
public interface LocationInfoMapper {

    LocationInfo toLocationInfo(Coordinate coordinate);

    Coordinate map(LocationInfo gps);

}
