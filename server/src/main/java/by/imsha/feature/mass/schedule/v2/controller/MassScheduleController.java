package by.imsha.feature.mass.schedule.v2.controller;

import api_specification.by.imsha.server.mass_schedule.v2.server.api.MassScheduleApiController;
import api_specification.by.imsha.server.mass_schedule.v2.server.model.MassScheduleDetails;
import api_specification.by.imsha.server.mass_schedule.v2.server.model.MassScheduleInput;
import by.imsha.feature.mass.schedule.v2.service.MassScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

import static by.imsha.feature.mass.schedule.v2.FeatureMetadata.COMPONENT_VERSION;

@Controller("massScheduleController" + COMPONENT_VERSION)
public class MassScheduleController extends MassScheduleApiController {

    private final MassScheduleService service;

    public MassScheduleController(NativeWebRequest request, MassScheduleService service) {
        super(request);
        this.service = service;
    }

    @Override
    public ResponseEntity<MassScheduleDetails> createMassSchedule(MassScheduleInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(input));
    }

    @Override
    public ResponseEntity<MassScheduleDetails> getMassSchedule(String id) {
        return ResponseEntity.ok(service.get(id));
    }

    @Override
    public ResponseEntity<Void> cancelMassOccurrence(String id, OffsetDateTime start) {
        service.cancelOccurrence(id, start);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> cancelMassSeries(String id) {
        service.cancelSeries(id);
        return ResponseEntity.noContent().build();
    }
}
