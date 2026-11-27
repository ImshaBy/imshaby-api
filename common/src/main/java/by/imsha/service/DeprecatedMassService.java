package by.imsha.service;

import by.imsha.domain.DeprecatedMass;
import by.imsha.domain.Mass;
import by.imsha.domain.dto.MassDay;
import by.imsha.domain.dto.MassNav;
import by.imsha.domain.dto.MassSchedule;
import by.imsha.domain.dto.UpdateMassInfo;
import by.imsha.domain.dto.mapper.DeprecatedMassMapper;
import by.imsha.domain.dto.mapper.MassInfoMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static by.imsha.utils.ServiceUtils.BEL_ZONE_ID;

@Service
@RequiredArgsConstructor
public class DeprecatedMassService {

    private final MassService massService;
    private final DeprecatedMassMapper deprecatedMassMapper;
    private final MassInfoMapper massInfoMapper;
    private final Validator validator;

    public List<MassDay> parishWeek(String parishId, LocalDate startDate) {
        return massService.parishWeek(parishId, startDate);
    }

    public MassSchedule weekMasses(String cityId, String parishId, LocalDate startDate,
                                   boolean online, boolean rorate, String massLang, boolean showPending) {
        return massService.weekMasses(cityId, parishId, startDate, online, rorate, massLang, showPending);
    }

    public DeprecatedMass createMass(DeprecatedMass mass) {
        Mass created = massService.createMass(deprecatedMassMapper.toMass(mass));
        return deprecatedMassMapper.toDeprecatedMass(created);
    }

    public DeprecatedMass updateMass(DeprecatedMass mass) {
        Mass updated = massService.updateMass(deprecatedMassMapper.toMass(mass));
        return deprecatedMassMapper.toDeprecatedMass(updated);
    }

    public Optional<DeprecatedMass> getMass(String id) {
        return massService.getMass(id).map(deprecatedMassMapper::toDeprecatedMass);
    }

    public void removeMass(String id) {
        massService.removeMass(id);
    }

    public List<String> removeMasses(String parishId) {
        return massService.removeMasses(parishId).stream().map(Mass::getId).toList();
    }

    public Triple<String, String, String> removeMassInterval(String id, LocalDate from, LocalDate to) {
        var start = from.atStartOfDay(BEL_ZONE_ID).toInstant();
        var endExclusive = to == null ? null : to.plusDays(1).atStartOfDay(BEL_ZONE_ID).toInstant();
        return massService.removeMass(id, start, endExclusive);
    }

    public List<DeprecatedMass> search(String filter, int offset, int limit, String sort) {
        List<Mass> masses = massService.search(filter, offset, limit, sort);
        return masses == null ? null : masses.stream().map(deprecatedMassMapper::toDeprecatedMass).toList();
    }

    public DeprecatedMass refreshMass(String id, UpdateMassInfo changes) {
        Mass stored = massService.requireMassForUpdate(id);
        var editable = deprecatedMassMapper.toDeprecatedMass(stored);
        if (changes != null) {
            massInfoMapper.updateMassFromDTO(changes, editable);
        }
        var violations = validator.validate(editable);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        Mass updated = massService.updateMass(deprecatedMassMapper.toMass(editable));
        return deprecatedMassMapper.toDeprecatedMass(updated);
    }

    public MassNav buildMassNavigationByFacetDistribution(final String cityId, final String parishId,
                                                                                   final boolean online, final String massLang,
                                                                                   final boolean rorate,
                                                          final Map<String, Map<String, Integer>> facetDistribution) {
        return massService.buildMassNavigationByFacetDistribution(
                cityId, parishId, online, massLang, rorate, facetDistribution);
    }
}
