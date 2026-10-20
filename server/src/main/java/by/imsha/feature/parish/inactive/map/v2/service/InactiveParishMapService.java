package by.imsha.feature.parish.inactive.map.v2.service;

import api_specification.by.imsha.server.inactive_parish_map.v2.server.model.InactiveParishMapItem;
import by.imsha.feature.parish.inactive.map.v2.mapper.InactiveParishMapMapper;
import by.imsha.service.ParishService;
import by.imsha.utils.UserLocaleHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static by.imsha.feature.parish.inactive.map.v2.FeatureMetadata.COMPONENT_VERSION;

@Service("inactiveParishMapService" + COMPONENT_VERSION)
@RequiredArgsConstructor
public class InactiveParishMapService {
    private final ParishService parishService;
    private final InactiveParishMapMapper mapper;

    public List<InactiveParishMapItem> getInactiveParishesForMap() {
        String locale = UserLocaleHolder.getUserLocale().orElse(null);
        return parishService.findInactiveForMap().stream()
                .map(parish -> mapper.map(parish, locale))
                .toList();
    }
}
