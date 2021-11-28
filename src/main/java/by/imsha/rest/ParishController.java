package by.imsha.rest;

import by.imsha.domain.LocalizedParish;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.*;
import by.imsha.exception.InvalidLocaleException;
import by.imsha.service.CityService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import by.imsha.utils.ServiceUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Parish services
 */

//@Api(name = "Parish services", description = "Methods for managing parishes", visibility = ApiVisibility.PUBLIC, stage = ApiStage.RC)
//@ApiVersion(since = "1.0")
//@ApiAuthNone
@RestController
@RequestMapping(value = "/api/parish")
public class ParishController extends AbstractRestHandler {

    @Autowired
    private ParishService parishService;

    @Autowired
    private MassService massService;

    @Autowired
    private CityService cityService;

    @Autowired
    private ScheduleFactory scheduleFactory;

    @RequestMapping(value = "",
            method = RequestMethod.POST,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.CREATED)
    public Parish createParish(@Valid @RequestBody Parish parish) {
        return parishService.createParish(parish);
    }

    @RequestMapping(value = "/{parishId}",
            method = RequestMethod.GET,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public EntityModel<Parish> retrieveParish(@PathVariable("parishId") String id,
                                              HttpServletRequest request, HttpServletResponse response) {
        Optional<Parish> parish = parishService.getParish(id);
        checkResourceFound(parish);
        EntityModel<Parish> parishResource = EntityModel.of(parish.get());
        return parishResource;
    }

    @RequestMapping(value = "/{parishId}",
            method = RequestMethod.PUT,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo updateParish(@PathVariable("parishId") String id, @RequestBody ParishInfo parishInfo) {
        Optional<Parish> parishToUpdate = this.parishService.getParish(id);
        checkResourceFound(parishToUpdate);
        Parish updatedParish = this.parishService.updateParish(parishInfo, parishToUpdate.get() );
        return new UpdateEntityInfo(updatedParish.getId(), UpdateEntityInfo.STATUS.UPDATED);
    }

    @RequestMapping(value = "/{parishId}/lang/{lc}",
            method = RequestMethod.PUT,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo createLocalizedParish(@PathVariable("parishId") String id, @PathVariable("lc") String lang,
                                                  @RequestBody LocalizedParishInfo localizedParishInfo){
        Optional<Parish> parishToUpdate = this.parishService.getParish(id);
        checkResourceFound(parishToUpdate);
        Locale localeObj = new Locale(lang);
        if(!LocaleUtils.isAvailableLocale(localeObj)){
            throw new InvalidLocaleException("Invalid lang specified : " + lang);
        }
        LocalizedParish localizedParish = new LocalizedParish(lang, id);
        localizedParish.setName(localizedParishInfo.getName());
        localizedParish.setAddress(localizedParishInfo.getAddress());
        localizedParish.setShortName(localizedParishInfo.getShortName());
        Parish updatedParish = this.parishService.updateLocalizedParishInfo(localizedParish, parishToUpdate.get());
        return new UpdateEntityInfo(updatedParish.getId(), UpdateEntityInfo.STATUS.UPDATED);
    }



    @RequestMapping(value = "/{parishId}",
            method = RequestMethod.DELETE,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    public UpdateEntityInfo removeParish(@PathVariable("parishId") String id, @RequestParam(value = "cascade", defaultValue = "false") Boolean cascade, HttpServletRequest request,
                                         HttpServletResponse response) {
        Optional<Parish> parish = this.parishService.getParish(id);
        checkResourceFound(parish);
        UpdateEntityInfo updateEntityInfo;

        if (cascade != null && cascade ) {
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
        } else{
          updateEntityInfo = new UpdateEntityInfo();
        }

        this.parishService.removeParish(id);
        updateEntityInfo.setId(id);
        updateEntityInfo.setStatus(UpdateEntityInfo.STATUS.DELETED.toString());
        return updateEntityInfo;
    }


    @RequestMapping(value = "/user/{userId}",
            method = RequestMethod.GET,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public EntityModel<Parish> retrieveParishByUser(@PathVariable("userId") String userId, HttpServletRequest request, HttpServletResponse response) {
        Parish parishByUser = this.parishService.getParishByUser(userId);
        checkResourceFound(Optional.ofNullable(parishByUser));
        EntityModel<Parish> parishResource = EntityModel.of(parishByUser);
        return parishResource;
    }


    @RequestMapping(value = "/week/expired",
            method = RequestMethod.GET,
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Set<ParishKeyUpdateInfo> retrieveParishKeysWithExpiredMasses(@CookieValue(value = "cityId", required = false) String cityId,
                                                          @RequestParam(value = "date", required = false) String day,
            HttpServletRequest request, HttpServletResponse response) {

        cityId = cityService.getCityIdOrDefault(cityId);
        List<Mass> masses = this.massService.getMassByCity(cityId); // TODO filter by date as well
        LocalDate date = ServiceUtils.formatDateString(day);

        MassSchedule massHolder = scheduleFactory.build(masses, date);

        List<MassInfo> weekMasses = massHolder.getMassesByDay().values().stream()
                .flatMap(timeMassValuesMap -> timeMassValuesMap.values().stream()
                                .flatMap(List::stream))
                .collect(Collectors.toList());

        Set<ParishKeyUpdateInfo> parishKeys = weekMasses.stream()
                .filter(massInfo -> massInfo.isNeedUpdate())
                .map(massInfo -> ParishService.extractParishKeyUpdateInfo(massInfo.getParish().getParishId()))
                .collect(Collectors.toSet());
        return parishKeys;
    }


    @RequestMapping(value = "",
            method = RequestMethod.GET,
            consumes = {"application/json"},
            produces = {"application/json"})
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<Parish> filterParishes(@RequestParam("filter") String filter,
                                       @RequestParam(value = "offset", required = false, defaultValue = "0") String page,
                                       @RequestParam(value = "limit", required = false, defaultValue = "10") String perPage,
                                       @RequestParam(value = "sort", required = false, defaultValue = "+name") String sorting,
                                       HttpServletRequest request, HttpServletResponse response) {
//        parishService.search(filter);
        log.info(filter);
        return parishService.search(filter, Integer.parseInt(page), Integer.parseInt(perPage), sorting);
    }

}
