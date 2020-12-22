package by.imsha.domain.dto.mapper;

import by.imsha.domain.Parish;
import by.imsha.domain.dto.ParishInfo;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * @author Alena Misan
 */
@Mapper( uses = {LocationInfoMapper.class},
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS ,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ParishInfoMapper {
    ParishInfoMapper MAPPER = Mappers.getMapper(ParishInfoMapper.class);
    @Mappings({
            @Mapping(source = "address", target = "address")
    })
    ParishInfo toParishInfo(Parish parish);

    Parish updateParishFromDTO(ParishInfo parishInfo, @MappingTarget Parish parish);

}
