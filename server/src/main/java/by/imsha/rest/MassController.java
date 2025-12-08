package by.imsha.rest;

import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.MassDay;
import by.imsha.domain.dto.MassNav;
import by.imsha.domain.dto.MassSchedule;
import by.imsha.domain.dto.UpdateEntitiesInfo;
import by.imsha.domain.dto.UpdateEntityInfo;
import by.imsha.domain.dto.UpdateMassInfo;
import by.imsha.domain.dto.mapper.MassInfoMapper;
import by.imsha.exception.InvalidDateIntervalException;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.mapper.MassCoordinatesMapper;
import by.imsha.meilisearch.model.SearchResultItem;
import by.imsha.meilisearch.reader.MassSearchFilter;
import by.imsha.meilisearch.reader.MeilisearchReader;
import by.imsha.meilisearch.reader.SearchResult;
import by.imsha.properties.ImshaProperties;
import by.imsha.rest.dto.MassCoordinatesResponse;
import by.imsha.security.ParishAuthorizationService;
import by.imsha.service.DefaultCityService;
import by.imsha.service.MassCoordinatesService;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import by.imsha.service.ScheduleFactory;
import by.imsha.utils.DateTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;


/**
 * @author Alena Misan
 */

@RestController
@RequestMapping(value = "/api/mass")
@Slf4j
public class MassController {

    //TODO всюду используется зона +3 (нужно хорошо всё обдумать и отрефакторить)
    private static final ZoneOffset BEL_ZONE_OFFSET = ZoneOffset.ofHours(3);

    @Autowired
    private MassService massService;


    @Autowired
    private ParishService parishService;

    @Autowired
    private DefaultCityService defaultCityService;

    @Autowired
    private ScheduleFactory scheduleFactory;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    @Autowired
    private ImshaProperties imshaProperties;

    @Autowired
    private MeilisearchReader meilisearchReader;

    @Autowired
    private MassInfoMapper massInfoMapper;

    @Autowired
    MassCoordinatesMapper massCoordinatesResponseMapper;

    @Autowired
    MassCoordinatesService massCoordinatesService;

    @Autowired
    private ParishAuthorizationService parishAuthorizationService;

    @PostMapping
    public ResponseEntity<Mass> createMass(@RequestBody final Mass mass) {
        // Проверка прав доступа к приходу
        parishAuthorizationService.checkParishAccess(mass.getParishId());
        
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
        // Проверка прав доступа к приходу
        parishAuthorizationService.checkParishAccess(mass.getParishId());
        
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
        
        // Проверка прав доступа к приходу
        parishAuthorizationService.checkParishAccess(massToUpdate.getParishId());

        if (massInfo == null) {
            massInfo = new UpdateMassInfo();
        }
        //TODO переделать мапперы на componentModel = "spring"
        massInfoMapper.updateMassFromDTO(massInfo, massToUpdate);
        final Mass updatedMass = this.massService.updateMass(massToUpdate);

        return ResponseEntity.ok(
                new UpdateEntityInfo(updatedMass.getId(), UpdateEntityInfo.STATUS.UPDATED)
        );
    }

    //TODO Нужно использовать /api/parish/{parishId}/confirm-relevance
    @Deprecated
    @PutMapping(params = "parishId")
    public ResponseEntity<UpdateEntitiesInfo> refreshMasses(@RequestParam("parishId") String parishId) {
        // Проверка прав доступа к приходу
        parishAuthorizationService.checkParishAccess(parishId);

        Parish parish = parishService.getParish(parishId).orElseThrow(ResourceNotFoundException::new);
        parish.setLastConfirmRelevance(dateTimeProvider.nowSystemDefaultZone());
        parishService.updateParish(parish);

        return ResponseEntity.ok(
                new UpdateEntitiesInfo(Collections.singletonList(parish.getId()),
                        UpdateEntitiesInfo.STATUS.UPDATED)
        );
    }


    @DeleteMapping("/{massId}")
    public ResponseEntity<UpdateEntityInfo> removeMass(@PathVariable("massId") String id) {
        final Mass mass = this.massService.getMass(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource not found"));
        
        // Проверка прав доступа к приходу
        parishAuthorizationService.checkParishAccess(mass.getParishId());

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
        
        // Проверка прав доступа к приходу
        parishAuthorizationService.checkParishAccess(mass.getParishId());

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
        // Проверка прав доступа к приходу
        parishAuthorizationService.checkParishAccess(parishId);
        
        if (!this.parishService.getParish(parishId).isPresent()) {
            throw new ResourceNotFoundException("resource not found");
        }

        final List<Mass> deletedMasses = this.massService.removeMasses(parishId);

        return ResponseEntity.ok(
                new UpdateEntitiesInfo(deletedMasses.stream().map(Mass::getId).collect(Collectors.toList()),
                        UpdateEntitiesInfo.STATUS.DELETED)
        );
    }

    @GetMapping("/map")
    public ResponseEntity<MassCoordinatesResponse> map(@RequestParam(value = "dateTimeFrom")
                                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                       OffsetDateTime dateTimeFrom,
                                                       @RequestParam(value = "dateTimeTo", required = false)
                                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                       OffsetDateTime dateTimeTo,
                                                       @RequestParam(value = "allParishes", required = false,
                                                               defaultValue = "false")
                                                       boolean allParishes
    ) {

        if (dateTimeTo == null) {
            //конец дня в РБ
            dateTimeTo = OffsetDateTime.of(
                    dateTimeFrom.withOffsetSameInstant(BEL_ZONE_OFFSET).toLocalDate(),
                    LocalTime.MAX,
                    BEL_ZONE_OFFSET
            );
        }

        return ResponseEntity.ok().body(
                massCoordinatesResponseMapper.map(
                        meilisearchReader.searchNearestMasses(MassSearchFilter.builder()
                                .dateTimeFrom(dateTimeFrom)
                                .dateTimeTo(dateTimeTo)
                                .build()),
                        allParishes ? massCoordinatesService.getNotApprovedAndNotDisabledParishesMassCoordinates() : List.of()
                )
        );
    }

    @GetMapping("/week-indexed")
    public ResponseEntity<MassSchedule> weekMassesFromMeilisearch(@CookieValue(value = "cityId", required = false) String cityId,
                                                                  @DateTimeFormat(pattern = "dd-MM-yyyy")
                                                                  @RequestParam(value = "date", required = false) LocalDate date,
                                                                  @RequestParam(value = "parishId", required = false) String parishId,
                                                                  @RequestParam(value = "online", defaultValue = "false") boolean onlineOnly,
                                                                  @RequestParam(value = "rorate", defaultValue = "false") boolean rorateOnly,
                                                                  @RequestParam(value = "massLang", required = false) String massLang,
                                                                  //TODO show pending немного не вписывается, либо добавляем его в SearchRecord
                                                                  @RequestHeader(name = "x-show-pending", required = false, defaultValue = "false") boolean showPending) {
        final LocalDate dateFrom = Optional.ofNullable(date).orElseGet(dateTimeProvider::today);
        //за 7 дней (неделю), включая дату dateFrom
        final LocalDate dateTo = dateFrom.plusDays(6);
        final Boolean rorate = rorateOnly ? true : null;
        final Boolean online = onlineOnly ? true : null;
        cityId = defaultCityService.getCityIdOrDefault(cityId);

        final SearchResult searchResult = meilisearchReader.searchAllMasses(MassSearchFilter.builder()
                .dateTimeFrom(LocalDateTime.of(dateFrom, LocalTime.MIN).atOffset(BEL_ZONE_OFFSET))
                .dateTimeTo(LocalDateTime.of(dateTo, LocalTime.MAX).atOffset(BEL_ZONE_OFFSET))
                .cityId(cityId)
                .parishId(parishId)
                .lang(massLang)
                .rorate(rorate)
                .online(online)
                .build());

        final List<SearchResultItem> resultItems = searchResult.hits();
        final Map<String, Map<String, Integer>> facetDistribution = searchResult.facetDistribution();

        final MassSchedule massSchedule = scheduleFactory.build(dateFrom, resultItems);

        massSchedule.createSchedule();

        final MassNav massNav = massService.buildMassNavigationByFacetDistribution(cityId, parishId, onlineOnly,
                massLang, rorateOnly, facetDistribution);

        massSchedule.setNav(massNav);

        return ResponseEntity.ok().body(massSchedule);
    }

    @GetMapping("/week")
    public ResponseEntity<MassSchedule> weekMasses(@CookieValue(value = "cityId", required = false) String cityId,
                                                   @DateTimeFormat(pattern = "dd-MM-yyyy")
                                                   @RequestParam(value = "date", required = false) LocalDate day,
                                                   @RequestParam(value = "parishId", required = false) String parishId,
                                                   @RequestParam(value = "online", defaultValue = "false") boolean online,
                                                   @RequestParam(value = "rorate", defaultValue = "false") boolean rorate,
                                                   @RequestParam(value = "massLang", required = false) String massLang,
                                                   @RequestHeader(name = "x-show-pending", required = false, defaultValue = "false") boolean showPending) {

        List<Mass> masses;
        if (StringUtils.isNotEmpty(parishId)) {
            Optional<Parish> parishOptional = parishService.getParish(parishId);

            if (!showPending) {
                //только APPROVED
                parishOptional = parishOptional.filter(parish -> Parish.State.APPROVED == parish.getState());
            }

            if (parishOptional.isPresent()) {
                cityId = parishOptional.get().getCityId();
                masses = this.massService.getMassByParish(parishId);
            } else {
                masses = Collections.emptyList();
            }
        } else {
            cityId = defaultCityService.getCityIdOrDefault(cityId);
            masses = this.massService.getMassByCity(cityId); // TODO filter by date as well

            if (!showPending) {
                final Set<String> notApprovedParishIds = parishService.getNotApprovedParishIds(cityId);
                masses = masses.stream().filter(mass -> !notApprovedParishIds.contains(mass.getParishId()))
                        .collect(Collectors.toList());
            }
        }

        if (online) {
            masses = massService.filterOutOnlyOnline(masses);
        }

        if (StringUtils.isNotEmpty(massLang)) {
            masses = massService.filterByMassLang(masses, massLang);
        }

        if (rorate) {
            masses = massService.filterOutRorateOnly(masses);
        }

        final LocalDate startDate = isNull(day) ? dateTimeProvider.today() : day;

        final MassSchedule massHolder = scheduleFactory.build(masses, startDate);

        massHolder.createSchedule();

        final MassNav massFilters = massService.buildMassNavigation(massHolder, cityId, parishId, Boolean.toString(online), massLang,
                rorate);

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

    /**
     * Расписание месс для парафии по ее API-ключу из параметра запроса
     *
     * @param paramsApiKey ключ парафии из параметра запроса (отличает этот эндпоинт от
     *                     {@link MassController#weekMasses(String, LocalDate, String, boolean, boolean, String, boolean)} )
     */
    @GetMapping(value = "/parish-week", params = "apiKey")
    public ResponseEntity<List<MassDay>> scheduleByParishKeyFromParams(@RequestParam(value = "apiKey") final String paramsApiKey) {
        return privateParishScheduleByApiKey(paramsApiKey);
    }

    /**
     * Расписание месс для парафии по ее API-ключу из заголовка запроса
     *
     * @param apiKey ключ парафии из заголовка запроса (отличает этот эндпоинт от
     *               {@link MassController#weekMasses(String, LocalDate, String, boolean, boolean, String, boolean)} )
     */
    @GetMapping(value = "/parish-week", headers = "parish-week-api-key")
    public ResponseEntity<List<MassDay>> scheduleByParishKeyFromHeaders(@RequestHeader(value = "parish-week-api-key") final String apiKey) {
        return privateParishScheduleByApiKey(apiKey);
    }

    /**
     * Расписание месс для парафии по api-ключу
     *
     * @param apiKey API-ключ парафии (соответсвует ключу одной из парафий)
     */
    private ResponseEntity<List<MassDay>> privateParishScheduleByApiKey(final String apiKey) {

        final String parishKey = Optional.ofNullable(apiKey)
                .map(imshaProperties.getParishWeekApiKeys().getMap()::get)
                .orElseThrow(ResourceNotFoundException::new);
        final Parish parish = parishService.findParishByKey(parishKey)
                .orElseThrow(ResourceNotFoundException::new);
        final List<Mass> masses = this.massService.getMassByParish(parish.getId());
        final LocalDate startDate = dateTimeProvider.today();

        final MassSchedule massHolder = scheduleFactory.build(masses, startDate);
        massHolder.createSchedule();

        return ResponseEntity.ok(
                massHolder.getSchedule()
        );
    }
}
