package by.imsha.feature.mass.schedule.v2.service;

import api_specification.by.imsha.server.mass_schedule.v2.server.model.MassScheduleDetails;
import api_specification.by.imsha.server.mass_schedule.v2.server.model.MassScheduleInput;
import by.imsha.feature.mass.schedule.v2.mapper.MassScheduleMapper;
import by.imsha.service.MassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static by.imsha.feature.mass.schedule.v2.FeatureMetadata.COMPONENT_VERSION;

@Service("massScheduleService" + COMPONENT_VERSION)
@RequiredArgsConstructor
public class MassScheduleService {

    private final MassService massService;
    private final MassScheduleMapper mapper;

    public MassScheduleDetails create(MassScheduleInput input) {
        var mass = mapper.toMass(input);
        return mapper.toDetails(massService.createMass(mass));
    }

    public MassScheduleDetails get(String id) {
        return mapper.toDetails(massService.getScheduledMass(id));
    }

    public void cancelOccurrence(String id, OffsetDateTime start) {
        massService.cancelOccurrence(id, start.toInstant());
    }

    public void cancelSeries(String id) {
        massService.cancelSeries(id);
    }
}
