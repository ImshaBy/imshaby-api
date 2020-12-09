package by.imsha.domain.dto.mapper;

import by.imsha.domain.Parish;
import by.imsha.domain.dto.ParishKeyUpdateInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * @author Alena Misan
 */
@Mapper
public interface ParishKeyUpdateInfoMapper {
    ParishKeyUpdateInfoMapper MAPPER = Mappers.getMapper(ParishKeyUpdateInfoMapper.class);

    @Mappings({
//     @Mapping(source = "id", target = "parishId")
    })
    ParishKeyUpdateInfo toParishKeyUpdateInfo(Parish parish);
}
