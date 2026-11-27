package by.imsha.feature.mass.schedule.v2.mapper;

import api_specification.by.imsha.server.mass_schedule.v2.server.model.MassScheduleDetails;
import api_specification.by.imsha.server.mass_schedule.v2.server.model.MassScheduleInput;
import by.imsha.domain.Mass;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.time.ZoneOffset;

import static by.imsha.feature.mass.schedule.v2.FeatureMetadata.COMPONENT_VERSION;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        implementationName = "massScheduleMapper" + COMPONENT_VERSION)
public interface MassScheduleMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "cityId", source = "cityId")
    @Mapping(target = "parishId", source = "parishId")
    @Mapping(target = "langCode", source = "langCode")
    @Mapping(target = "duration", source = "duration")
    @Mapping(target = "notes", source = "notes")
    @Mapping(target = "online", source = "online")
    @Mapping(target = "rorate", source = "rorate")
    @Mapping(target = "calendar", source = "input")
    Mass toMass(MassScheduleInput input);

    default Mass.CalendarDescription toCalendar(MassScheduleInput input) {
        return new Mass.CalendarDescription(input.getDtstart().toInstant(), input.getRrule());
    }

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "dtstart", ignore = true)
    @Mapping(target = "rrule", source = "calendar.rrule")
    @Mapping(target = "exdates", ignore = true)
    MassScheduleDetails toDetails(Mass mass);

    @AfterMapping
    default void mapCalendarDates(Mass mass, @MappingTarget MassScheduleDetails details) {
        var calendar = mass.getCalendar();
        details.setDtstart(calendar.dtstart().atOffset(ZoneOffset.UTC));
        details.setExdates(calendar.exdates().stream()
                .map(instant -> instant.atOffset(ZoneOffset.UTC)).toList());
    }
}
