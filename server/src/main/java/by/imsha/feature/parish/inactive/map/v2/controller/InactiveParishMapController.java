package by.imsha.feature.parish.inactive.map.v2.controller;

import api_specification.by.imsha.server.inactive_parish_map.v2.server.api.InactiveParishMapApiController;
import api_specification.by.imsha.server.inactive_parish_map.v2.server.model.InactiveParishMapItem;
import by.imsha.feature.parish.inactive.map.v2.service.InactiveParishMapService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;

import static by.imsha.feature.parish.inactive.map.v2.FeatureMetadata.COMPONENT_VERSION;

@Controller("inactiveParishMapController" + COMPONENT_VERSION)
public class InactiveParishMapController extends InactiveParishMapApiController {
    private final InactiveParishMapService service;

    public InactiveParishMapController(NativeWebRequest request, InactiveParishMapService service) {
        super(request);
        this.service = service;
    }

    @Override
    public ResponseEntity<List<InactiveParishMapItem>> getInactiveParishesForMap() {
        return ResponseEntity.ok(service.getInactiveParishesForMap());
    }
}
