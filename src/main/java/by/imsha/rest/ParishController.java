package by.imsha.rest;

import by.imsha.domain.City;
import by.imsha.domain.LocalizedParish;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.*;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.service.CityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import by.imsha.utils.DateTimeProvider;
import by.imsha.validation.common.AvailableLocale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Parish services
 */
@RestController
@RequestMapping(value = "/api/parish")
@Validated
@Slf4j
public class ParishController {

    @Autowired
    private ParishService parishService;

    @Autowired
    private MassService massService;

    @Autowired
    private CityService cityService;

    @Autowired
    private ScheduleFactory scheduleFactory;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    @PostMapping
    public ResponseEntity<Parish> createParish(@Valid @RequestBody Parish parish) {
        parish.setState(Parish.State.PENDING);
        parish.setLastConfirmRelevance(dateTimeProvider.now());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parishService.createParish(parish));
    }

    @GetMapping("/{parishId}")
    public ResponseEntity<Parish> retrieveParish(@PathVariable("parishId") String parishId) {
        return parishService.getParish(parishId)
                .map(ResponseEntity::ok)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @PutMapping("/{parishId}")
    public ResponseEntity<UpdateEntityInfo> updateParish(@PathVariable("parishId") String id, @RequestBody ParishInfo parishInfo) {
        final Parish parishToUpdate = this.parishService.getParish(id)
                .orElseThrow(ResourceNotFoundException::new);

        final Parish updatedParish = this.parishService.updateParish(parishInfo, parishToUpdate);

        return ResponseEntity.ok(
                new UpdateEntityInfo(updatedParish.getId(), UpdateEntityInfo.STATUS.UPDATED)
        );
    }

    @GetMapping(value = "/{parishId}/state")
    public ResponseEntity<ParishStateInfo> getParishState(@PathVariable("parishId") String parishId) {
        final Parish parish = this.parishService.getParish(parishId)
                .orElseThrow(ResourceNotFoundException::new);

        return ResponseEntity.ok(
                ParishStateInfo.builder()
                        .state(parish.getState())
                        .build()
        );
    }

    @PutMapping(value = "/{parishId}/state")
    public ResponseEntity<UpdateEntityInfo> updateParishState(@PathVariable("parishId") String parishId,
                                                              @Valid @RequestBody ParishStateInfo parishStateInfo) {
        final Parish parish = this.parishService.getParish(parishId)
                .orElseThrow(ResourceNotFoundException::new);

        parish.setState(parishStateInfo.getState());

        final Parish updatedParish = parishService.updateParish(parish);
        return ResponseEntity.ok(
                new UpdateEntityInfo(updatedParish.getId(), UpdateEntityInfo.STATUS.UPDATED)
        );
    }

    @PutMapping(value = "/{parishId}/lang/{locale}")
    public ResponseEntity<UpdateEntityInfo> createLocalizedParish(@PathVariable("parishId") String id,
                                                                  @AvailableLocale(field = "locale", message = "PARISH.001")
                                                                  @PathVariable("locale") Locale locale,
                                                                  @RequestBody @Valid LocalizedParishInfo localizedParishInfo) {
        final Parish parishToUpdate = this.parishService.getParish(id)
                .orElseThrow(ResourceNotFoundException::new);

        final LocalizedParish localizedParish = new LocalizedParish(locale.getLanguage(), id);
        localizedParish.setName(localizedParishInfo.getName());
        localizedParish.setAddress(localizedParishInfo.getAddress());
        localizedParish.setShortName(localizedParishInfo.getShortName());

        final Parish updatedParish = this.parishService.updateLocalizedParishInfo(localizedParish, parishToUpdate);

        return ResponseEntity.ok(
                new UpdateEntityInfo(updatedParish.getId(), UpdateEntityInfo.STATUS.UPDATED)
        );
    }


    @DeleteMapping("/{parishId}")
    public ResponseEntity<UpdateEntityInfo> removeParish(@PathVariable("parishId") String id,
                                                         @RequestParam(value = "cascade", defaultValue = "false") Boolean cascade) {
        if (!this.parishService.getParish(id).isPresent()) {
            throw new ResourceNotFoundException();
        }

        final UpdateEntityInfo updateEntityInfo;

        if (cascade != null && cascade) {
            updateEntityInfo = new CascadeUpdateEntityInfo();
            List<UpdateEntityInfo> massEntityInfos = new ArrayList<>();
            ((CascadeUpdateEntityInfo) updateEntityInfo).setRelatedEntities(massEntityInfos);
            List<Mass> parishMasses = massService.getMassByParish(id);
            List<String> massIds = new ArrayList<>();
            for (Mass parishMass : parishMasses) {
                massIds.add(parishMass.getId());
                massService.removeMass(parishMass);
                massEntityInfos.add(new UpdateEntityInfo(parishMass.getId(), UpdateEntityInfo.STATUS.DELETED));
            }
            log.warn(String.format("Cascade remove is executed for mass list: %s", massIds));
        } else {
            updateEntityInfo = new UpdateEntityInfo();
        }

        this.parishService.removeParish(id);

        updateEntityInfo.setId(id);
        updateEntityInfo.setStatus(UpdateEntityInfo.STATUS.DELETED.toString());

        return ResponseEntity.ok(
                updateEntityInfo
        );
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<Parish> retrieveParishByUser(@PathVariable("userId") String userId) {
        return Optional.ofNullable(this.parishService.getParishByUser(userId))
                .map(ResponseEntity::ok)
                .orElseThrow(ResourceNotFoundException::new);
    }

    //TODO вынести логику в сервис и тогда покрыть контроллер тестами
    @GetMapping("/week/expired")
    public ResponseEntity<Set<ParishKeyUpdateInfo>> retrieveParishKeysWithExpiredMasses(
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            @RequestParam(value = "date", required = false) LocalDate date) {

        List<City> allCities = cityService.getAllCities();
        Set<String> allPendingParishes = allCities.stream()
                .map(city -> parishService.getPendingParishIds(city.getId()))
                .flatMap(parishIds -> parishIds.stream())
                .collect(Collectors.toSet());

        List<Mass> allMasses = allCities.stream()
                .map(city -> massService.getMassByCity(city.getId()))
                .flatMap(masses -> masses.stream())
                .filter(mass -> !allPendingParishes.contains(mass.getParishId()))
                .collect(Collectors.toList());

        if (date == null) {
            date = dateTimeProvider.today();
        }

        MassSchedule massHolder = scheduleFactory.build(allMasses, date);

        List<MassInfo> weekMasses = massHolder.getMassesByDay().values().stream()
                .flatMap(timeMassValuesMap -> timeMassValuesMap.values().stream()
                                .flatMap(List::stream))
                .collect(Collectors.toList());

        Set<ParishKeyUpdateInfo> parishKeys = weekMasses.stream()
                .filter(massInfo -> massInfo.getParish().isNeedUpdate())
                .map(massInfo -> ParishService.extractParishKeyUpdateInfo(massInfo.getParish().getParishId()))
                .collect(Collectors.toSet());
        return ResponseEntity.ok(
                parishKeys
        );
    }


    @GetMapping
    public ResponseEntity<List<Parish>> filterParishes(@RequestParam("filter") String filter,
                                                       @RequestParam(value = "offset", required = false, defaultValue = "0") int page,
                                                       @RequestParam(value = "limit", required = false, defaultValue = "10") int perPage,
                                                       @RequestParam(value = "sort", required = false, defaultValue = "+name") String sorting) {
        log.info(filter);

        return ResponseEntity.ok(
                parishService.search(filter, page, perPage, sorting)
        );
    }

    @PutMapping("/{parishId}/confirm/relevance")
    public ResponseEntity<UpdateEntitiesInfo> confirmRelevance(@PathVariable("parishId") String parishId) {

        Parish parish = parishService.getParish(parishId).orElseThrow(ResourceNotFoundException::new);
        parish.setLastConfirmRelevance(dateTimeProvider.now());
        parishService.updateParish(parish);

        return ResponseEntity.ok(
                new UpdateEntitiesInfo(Collections.singletonList(parish.getId()),
                        UpdateEntitiesInfo.STATUS.UPDATED)
        );
    }

}
