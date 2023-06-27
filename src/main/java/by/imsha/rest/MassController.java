package by.imsha.rest;

import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.*;
import by.imsha.domain.dto.mapper.MassInfoMapper;
import by.imsha.exception.InvalidDateIntervalException;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.service.CityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import by.imsha.utils.ServiceUtils;
import by.imsha.utils.DateTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;


/**
 * @author Alena Misan
 */

@RestController
@RequestMapping(value = "/api/mass")
@Slf4j
public class MassController {

    @Autowired
    private MassService massService;


    @Autowired
    private ParishService parishService;

    @Autowired
    private CityService cityService;

    @Autowired
    private ScheduleFactory scheduleFactory;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    @PostMapping
    public ResponseEntity<Mass> createMass(@RequestBody final Mass mass) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(massService.createMass(mass));
    }

    @GetMapping("/{massId}")
    public ResponseEntity<Mass> retrieveMass(@PathVariable("massId") final String id) {
        return massService.getMass(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("resource not found"));
    }

    @PutMapping("/{massId}")
    public ResponseEntity<UpdateEntityInfo> updateMass(@PathVariable("massId") String id, @RequestBody Mass mass) {
        //из-за кэша не переделывал проверку на existsById
        if (!this.massService.getMass(id).isPresent()) {
            throw new ResourceNotFoundException("resource not found");
        }

        mass.setId(id);
        final Mass updatedMass = this.massService.updateMass(mass);

        return ResponseEntity.ok(
                new UpdateEntityInfo(updatedMass.getId(), UpdateEntityInfo.STATUS.UPDATED)
        );
    }


    @PutMapping("/refresh/{massId}")
    public ResponseEntity<UpdateEntityInfo> refreshMass(@PathVariable("massId") final String id, @RequestBody(required = false) UpdateMassInfo massInfo) {
        final Mass massToUpdate = this.massService.getMass(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource not found"));

        if (massInfo == null) {
            massInfo = new UpdateMassInfo();
        }
        //TODO переделать мапперы на componentModel = "spring"
        MassInfoMapper.MAPPER.updateMassFromDTO(massInfo, massToUpdate);
        final Mass updatedMass = this.massService.updateMass(massToUpdate);

        return ResponseEntity.ok(
                new UpdateEntityInfo(updatedMass.getId(), UpdateEntityInfo.STATUS.UPDATED)
        );
    }

    @PutMapping(params = "parishId")
    public ResponseEntity<UpdateEntitiesInfo> refreshMasses(@RequestParam("parishId") String parishId) {
        final List<Mass> massesToRefresh = massService.getMassByParish(parishId);

        for (Mass massForUpdate : massesToRefresh) {
            MassInfoMapper.MAPPER.updateMassFromDTO(new UpdateMassInfo(), massForUpdate);
            this.massService.updateMass(massForUpdate);
        }

        return ResponseEntity.ok(
                new UpdateEntitiesInfo(massesToRefresh.stream().map(Mass::getId).collect(Collectors.toList()),
                        UpdateEntitiesInfo.STATUS.UPDATED)
        );
    }


    @DeleteMapping("/{massId}")
    public ResponseEntity<UpdateEntityInfo> removeMass(@PathVariable("massId") String id) {
        final Mass mass = this.massService.getMass(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource not found"));

        this.massService.removeMass(mass);

        return ResponseEntity.ok(
                new UpdateEntityInfo(id, UpdateEntityInfo.STATUS.DELETED)
        );
    }

    @DeleteMapping(path = "/{massId}", params = "from")
    public ResponseEntity<List<UpdateEntityInfo>> removeMassByTimeInterval(@PathVariable("massId") String id,
                                                                           @DateTimeFormat(pattern = "dd-MM-yyyy")
                                                                           @RequestParam("from") LocalDate fromDate,
                                                                           @DateTimeFormat(pattern = "dd-MM-yyyy")
                                                                           @RequestParam(name = "to", required = false) LocalDate toDate) {
        Mass mass = this.massService.getMass(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource not found"));

        if (toDate != null && fromDate.isAfter(toDate)) {
            throw new InvalidDateIntervalException("Invalid dates", "from", "MASS.012");
        }

        Triple<String, String, String> removalResult = this.massService.removeMass(mass, fromDate, toDate);
        List<UpdateEntityInfo> updateEntityInfos = new ArrayList<>();
        String infoId = removalResult.getLeft();
        if (infoId != null) {
            updateEntityInfos.add(new UpdateEntityInfo(infoId, UpdateEntityInfo.STATUS.UPDATED));
        }
        infoId = removalResult.getMiddle();
        if (infoId != null) {
            updateEntityInfos.add(new UpdateEntityInfo(infoId, UpdateEntityInfo.STATUS.CREATED));
        }
        infoId = removalResult.getRight();
        if (infoId != null) {
            updateEntityInfos.add(new UpdateEntityInfo(infoId, UpdateEntityInfo.STATUS.DELETED));
        }
        return ResponseEntity.ok(updateEntityInfos);
    }

    @DeleteMapping(params = "parishId")
    public ResponseEntity<UpdateEntitiesInfo> removeMasses(@RequestParam(value = "parishId") String parishId) {
        if (!this.parishService.getParish(parishId).isPresent()) {
            throw new ResourceNotFoundException("resource not found");
        }

        final List<Mass> deletedMasses = this.massService.removeMasses(parishId);

        return ResponseEntity.ok(
                new UpdateEntitiesInfo(deletedMasses.stream().map(Mass::getId).collect(Collectors.toList()),
                        UpdateEntitiesInfo.STATUS.DELETED)
        );
    }


    @GetMapping("/week")
    public ResponseEntity<MassSchedule> weekMasses(@CookieValue(value = "cityId", required = false) String cityId,
                                                   @DateTimeFormat(pattern = "dd-MM-yyyy")
                                                   @RequestParam(value = "date", required = false) LocalDate day,
                                                   @RequestParam(value = "parishId", required = false) String parishId,
                                                   @RequestParam(value = "online", defaultValue = "false") boolean online,
                                                   @RequestParam(value = "massLang", required = false) String massLang,
                                                   @RequestHeader(name = "x-show-pending", required = false, defaultValue = "false") boolean showPending) {

        List<Mass> masses;
        if (StringUtils.isNotEmpty(parishId)) {
            Optional<Parish> parishOptional = parishService.getParish(parishId);

            if (!showPending) {
                parishOptional = parishOptional.filter(parish -> Parish.State.PENDING != parish.getState());
            }

            if (parishOptional.isPresent()) {
                cityId = parishOptional.get().getCityId();
                masses = this.massService.getMassByParish(parishId);
            } else {
                masses = Collections.emptyList();
            }
        } else {
            cityId = cityService.getCityIdOrDefault(cityId);
            masses = this.massService.getMassByCity(cityId); // TODO filter by date as well

            if (!showPending) {
                final Set<String> pendingParishIds = parishService.getPendingParishIds(cityId);
                masses = masses.stream().filter(mass -> !pendingParishIds.contains(mass.getParishId()))
                        .collect(Collectors.toList());
            }
        }

        if (online) {
            masses = massService.filterOutOnlyOnline(masses);
        }

        if (StringUtils.isNotEmpty(massLang)) {
            masses = massService.filterByMassLang(masses, massLang);
        }

        final LocalDate startDate = isNull(day) ? dateTimeProvider.today() : day;

        final MassSchedule massHolder = scheduleFactory.build(masses, startDate);

        massHolder.createSchedule();

        final MassNav massFilters = massService.buildMassNavigation(massHolder, cityId, parishId, Boolean.toString(online), massLang);

        massHolder.setNav(massFilters);

        log.debug("{} masses found: {}. Scheduler is built.", masses.size(), masses);

        return ResponseEntity.ok(
                massHolder
        );
    }

    @GetMapping
    public ResponseEntity<List<Mass>> filterMasses(@RequestParam("filter") String filter,
                                                   @RequestParam(value = "offset", required = false, defaultValue = "0") Integer page,
                                                   @RequestParam(value = "limit", required = false, defaultValue = "10") Integer perPage,
                                                   @RequestParam(value = "sort", required = false, defaultValue = "+name") String sorting) {
        return ResponseEntity.ok(
                massService.search(filter, page, perPage, sorting)
        );
    }


}
