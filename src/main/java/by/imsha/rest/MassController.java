package by.imsha.rest;

import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.*;
import by.imsha.exception.InvalidDateIntervalException;
import by.imsha.service.CityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import by.imsha.utils.ServiceUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author Alena Misan
 */

@RestController
@RequestMapping(value = "/api/mass")
public class MassController extends AbstractRestHandler {

    @Autowired
    private MassService massService;


    @Autowired
    private ParishService parishService;

    @Autowired
    private CityService cityService;

    @Autowired
    private ScheduleFactory scheduleFactory;


    @RequestMapping(value = "",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Mass> createMass(@Validated @RequestBody Mass mass, HttpServletRequest request, HttpServletResponse response){
        return ResponseEntity.ok().body(massService.createMass(mass));
    }

    @RequestMapping(value = "/{massId}",
            method = RequestMethod.GET,
            produces = {"application/json", "application/xml"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public EntityModel<Mass> retrieveMass(@PathVariable("massId") String id,
                                          HttpServletRequest request, HttpServletResponse response){
        Optional<Mass> mass = massService.getMass(id);
        checkResourceFound(mass);
        EntityModel<Mass> massResource = EntityModel.of(mass.get());
        return massResource;
    }

    @RequestMapping(value = "/{massId}",
            method = RequestMethod.PUT,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo updateMass(@PathVariable("massId") String id,@Validated @RequestBody Mass mass) {
        Optional<Mass> massForUpdate = this.massService.getMass(id);
        checkResourceFound(massForUpdate);
        // TODO check implementation??
        mass.setId(id);
        Mass updatedMass = this.massService.updateMass(mass);
        return new UpdateEntityInfo(updatedMass.getId(), UpdateEntityInfo.STATUS.UPDATED);
    }


    @RequestMapping(value = "/refresh/{massId}",
            method = RequestMethod.PUT,
            consumes = {"application/json"},
            produces = {"application/json"}
            )
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo refreshMass(@PathVariable("massId") String id,@Validated @RequestBody (required = false) UpdateMassInfo massInfo) {
        Optional<Mass> massForUpdate = this.massService.getMass(id);
        checkResourceFound(massForUpdate);
        if(massInfo == null){
            massInfo = new UpdateMassInfo();
        }
        Mass updatedMass = this.massService.updateMass(massInfo, massForUpdate.get());
        return new UpdateEntityInfo(updatedMass.getId(), UpdateEntityInfo.STATUS.UPDATED);
    }

    @RequestMapping(
            method = RequestMethod.PUT,
            consumes = {"application/json"},
            produces = {"application/json"},
            params = {"parishId"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntitiesInfo refreshMasses(@RequestParam("parishId") String parishId) {
        List<Mass> massesToRefresh = massService.getMassByParish(parishId);
        for (Mass massForUpdate : massesToRefresh) {
            this.massService.updateMass(new UpdateMassInfo(), massForUpdate);
        }
        return new UpdateEntitiesInfo(massesToRefresh.stream()
                .map(Mass::getId).collect(Collectors.toList()), UpdateEntitiesInfo.STATUS.UPDATED);
    }


    @RequestMapping(value = "/{massId}",
            method = RequestMethod.DELETE,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo removeMass(@PathVariable("massId") String id) {
        Optional<Mass> mass = this.massService.getMass(id);
        checkResourceFound(mass);
        this.massService.removeMass(mass.get());
        return new UpdateEntityInfo(id, UpdateEntityInfo.STATUS.DELETED);
    }

    @RequestMapping(value = "/{massId}",
            method = RequestMethod.DELETE,
            produces = {"application/json"},
            params = {"from"})
    @ResponseStatus(HttpStatus.OK)
    public List<UpdateEntityInfo> removeMassByTimeInterval(@PathVariable("massId") String id,
                                                           @RequestParam("from") String fromDateStr) {
        return removeMassByTimeInterval(id, fromDateStr, null);
    }

    @RequestMapping(value = "/{massId}",
            method = RequestMethod.DELETE,
            produces = {"application/json"},
            params = {"from", "to"})
    @ResponseStatus(HttpStatus.OK)
    public List<UpdateEntityInfo> removeMassByTimeInterval(@PathVariable("massId") String id,
                                                     @RequestParam("from") String fromDateStr,
                                                     @RequestParam(name = "to", required = false) String toDateStr) {
        Optional<Mass> mass = this.massService.getMass(id);
        checkResourceFound(mass);
        LocalDate fromDate = ServiceUtils.formatDateString(fromDateStr);
        LocalDate toDate = toDateStr == null ? null : ServiceUtils.formatDateString(toDateStr);
        if (toDate != null && fromDate.isAfter(toDate)) {
            throw new InvalidDateIntervalException(String.format("Invalid date interval bounds (from: %s, to: %s), " +
                    "the from-date should be equal or less than to-date!", fromDateStr, toDateStr));
        }
        Triple<String, String, String> removalResult = this.massService.removeMass(mass.get(), fromDate, toDate);
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
        return updateEntityInfos;
    }

    @RequestMapping(method = RequestMethod.DELETE,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntitiesInfo removeMasses(@RequestParam(value="parishId", required = true) String parishId) {
        Optional<Parish> massParish = this.parishService.getParish(parishId);
        checkResourceFound(massParish);
        List<Mass> deletedMasses = this.massService.removeMasses(parishId);
        return new UpdateEntitiesInfo(deletedMasses.stream()
                .map(Mass::getId).collect(Collectors.toList()), UpdateEntitiesInfo.STATUS.DELETED);
    }



    @RequestMapping(value = "/week",
            method = RequestMethod.GET,
            produces = {"application/json", "application/xml"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public EntityModel<MassSchedule> weekMasses(@CookieValue(value = "cityId", required = false) String cityId, @RequestParam(value = "date", required = false) String day,
                                                @RequestParam(value = "parishId", required = false) String parishId, @RequestParam(value = "online", defaultValue = "false") String online,
                                                @RequestParam(value = "massLang", required = false) String massLang,
                                                @RequestHeader(name = "x-show-pending", required = false, defaultValue = "false") boolean showPending) {

        List<Mass> masses;
        if(StringUtils.isNotEmpty(parishId)){
            Optional<Parish> parishOptional = parishService.getParish(parishId);

            if (!showPending) {
                parishOptional = parishOptional.filter(parish -> Parish.State.PENDING != parish.getState());
            }

            if(parishOptional.isPresent()){
                cityId = parishOptional.get().getCityId();
                masses = this.massService.getMassByParish(parishId);
            }else{
                masses = Collections.emptyList();
            }
        }else{
            cityId = cityService.getCityIdOrDefault(cityId);
            masses = this.massService.getMassByCity(cityId); // TODO filter by date as well

            if (!showPending) {
                final Set<String> pendingParishIds = parishService.getPendingParishIds(cityId);
                masses = masses.stream().filter(mass -> !pendingParishIds.contains(mass.getParishId()))
                        .collect(Collectors.toList());
            }
        }

        if(Boolean.parseBoolean(online)){
            masses = massService.filterOutOnlyOnline(masses);
        }

        if(StringUtils.isNotEmpty(massLang)){
            masses = massService.filterByMassLang(masses, massLang);
        }

        LocalDate date = ServiceUtils.formatDateString(day);

        MassSchedule massHolder = scheduleFactory.build(masses, date);

        massHolder.createSchedule();
        MassNav massFilters;
        if(masses.isEmpty()){
            massFilters = MassNav.EMPTY_NAV;
        }else{
            massFilters = massService.buildMassNavigation(massHolder, cityId, parishId, online, massLang);
        }

        massHolder.setNav(massFilters);

        if(log.isDebugEnabled()){
            log.debug(String.format("%s masses found: %s. Scheduler is built.", masses.size(), masses));
        }
        EntityModel<MassSchedule> massResource = EntityModel.of(massHolder);
        // TODO add links
//        massResource.add(linkTo(methodOn(MassController.class).retrieveMassByParish(parishId,request, response)).withSelfRel());
        return massResource;
    }





    @RequestMapping(value = "",
            method = RequestMethod.GET,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Mass> filterMasses(@RequestParam("filter") String filter,
                                       @RequestParam(value="offset", required = false, defaultValue = "0") String page,
                                       @RequestParam(value="limit", required = false, defaultValue = "10") String perPage,
                                       @RequestParam(value="sort", required = false, defaultValue = "+name") String sorting){
        return massService.search(filter, Integer.parseInt(page), Integer.parseInt(perPage), sorting);
    }



}
