package by.imsha;

import by.imsha.domain.dto.MassSchedule;
import by.imsha.meilisearch.reader.SearchResult;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

//@Mapper(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
//        componentModel = "spring")
public interface SearchResultMapper {

//    TODO нужно самостоятельно составить такой объект, не используя существующие фабрики
//    MassSchedule toMassSchedule(SearchResult searchResult);
}
