package by.imsha.service;

import by.imsha.domain.City;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.MassDay;
import by.imsha.domain.dto.MassFilterType;
import by.imsha.domain.dto.MassFilterValue;
import by.imsha.domain.dto.MassInfo;
import by.imsha.domain.dto.MassNav;
import by.imsha.domain.dto.MassSchedule;
import by.imsha.exception.InvalidFilterException;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.mass.scheduling.domain.CalendarSchedule;
import by.imsha.mass.scheduling.application.ScheduleConflict;
import by.imsha.mass.scheduling.domain.ScheduleRule;
import by.imsha.meilisearch.model.SearchRecord;
import by.imsha.repository.MassRepository;
import by.imsha.repository.ParishRepository;
import by.imsha.utils.ServiceUtils;
import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import com.github.rutledgepaulv.rqe.pipes.QueryConversionPipeline;
import jakarta.validation.Valid;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import java.text.Collator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static by.imsha.utils.LocalizedUtils.getLocalizedCityName;
import static by.imsha.utils.LocalizedUtils.getLocalizedParishName;
import static by.imsha.utils.LocalizedUtils.getLocalizedParishShortName;
import static by.imsha.utils.UserLocaleHolder.getUserLocale;

/**
 * @author Alena Misan
 */

@Service
@Validated
public class MassService {
    @Autowired
    private MassCalendarMapper massCalendarMapper;

    private static Logger logger = LoggerFactory.getLogger(MassService.class);

    /**
     * Мап, для кэширования компараторов для различных локалей
     */
    private static final Map<String, Comparator<MassFilterValue>> COMPARATOR_CACHE = new HashMap<>();

    @Autowired
    private CityService cityService;

    @Autowired
    private ParishRepository parishRepository;

    @Autowired
    private ParishService parishService;

    @Autowired
    private DeprecatedScheduleFactory scheduleFactory;

    private QueryConversionPipeline pipeline = QueryConversionPipeline.defaultPipeline();

    private MongoVisitor mongoVisitor = new MongoVisitor();

    @Autowired
    private MassRepository massRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId")
    })
    public Mass createMass(@Valid Mass mass) {
        return massRepository.insert(mass);
    }

    public Mass getScheduledMass(String id) {
        return requireActiveMass(id);
    }

    @CacheEvict(cacheNames = "massCache", allEntries = true)
    public void cancelOccurrence(String id, Instant start) {
        cancelStoredOccurrence(requireActiveMass(id), start);
    }

    private void cancelStoredOccurrence(Mass mass, Instant start) {
        String id = mass.getId();
        var stored = mass.getCalendar();
        ScheduleRule rule = massCalendarMapper.toScheduleRule(mass.getCalendar());
        if (!rule.contains(start)) {
            throw new ResourceNotFoundException("mass occurrence not found");
        }

        Query query = activeMass(id);
        Update update = new Update().set("lastModifiedDate", LocalDateTime.now());
        query.addCriteria(Criteria.where("calendar.dtstart").is(stored.dtstart())
                .and("calendar.rrule").is(stored.rrule()));
        update.addToSet("calendar.exdates", start);
        if (mongoTemplate.updateFirst(query, update, Mass.class).getMatchedCount() == 0) {
            throw new ResourceNotFoundException("mass occurrence not found");
        }
    }

    @CacheEvict(cacheNames = "massCache", allEntries = true)
    public synchronized void cancelSeries(String id) {
        var update = new Update().set("deleted", true).set("lastModifiedDate", LocalDateTime.now());
        if (mongoTemplate.updateFirst(activeMass(id), update, Mass.class).getMatchedCount() == 0) {
            throw new ResourceNotFoundException("resource not found");
        }
    }

    private static Query activeMass(String id) {
        return Query.query(Criteria.where("_id").is(id).and("deleted").ne(true));
    }

    private Mass requireActiveMass(String id) {
        return massRepository.findById(id).filter(mass -> !mass.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("resource not found"));
    }

    @Cacheable(cacheNames = "massCache", key = "'massesByParish:' + #parishId")
    public List<Mass> getMassByParish(String parishId){
        List<Mass> masses = massRepository.findByParishId(parishId);
        if(CollectionUtils.isEmpty(masses)){
            if(logger.isWarnEnabled()){
                logger.warn(String.format("No masses found with parish id = %s", parishId));
            }
        }
        return masses;
    }

    @Cacheable(cacheNames = "massCache", key = "'massesByCity:' + #cityId")
    public List<Mass> getMassByCity(String cityId){
//        TODO check index for cityID and deleted.
        List<Mass> masses = massRepository.findByCityIdAndDeleted(cityId, false);
        if(CollectionUtils.isEmpty(masses)){
            if(logger.isWarnEnabled()){
                logger.warn(String.format("No masses found with city id = %s, and deleted = %s", cityId, Boolean.FALSE));
            }
        }
        return masses;
    }

    @Cacheable(cacheNames = "massCache")
    public Optional<Mass> getMass(String id){
        return getMassForUpdate(id);
    }

    private Optional<Mass> getMassForUpdate(String id) {
        return massRepository.findById(id);
    }

    public Mass requireMassForUpdate(String id) {
        return getMassForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource not found"));
    }

    public List<Mass> search(String filter, int offset, int limit, String sort){
        Query query = prepareQuery(filter, offset, limit, sort);
        if (query == null) return null;
        return mongoTemplate.find(query, Mass.class);
    }

    private Query prepareQuery(String filter, int offset, int limit, String sort) {
        if(StringUtils.isBlank(filter)){
            if(logger.isInfoEnabled()){
                logger.info("No searching masses: query is blank");
            }
            return null;
        }
        int[] offsetAndLimit = ServiceUtils.calculateOffsetAndLimit(offset, limit);
        try {
            Condition<GeneralQueryBuilder> condition = pipeline.apply(filter, Mass.class);
            return ServiceUtils.buildMongoQuery(sort, offsetAndLimit[0], offsetAndLimit[1], condition, mongoVisitor);
        } catch (RuntimeException exception) {
            throw new InvalidFilterException(filter, exception);
        }
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "#p0.id"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId")
    })
    public synchronized Mass updateMass(@Valid Mass mass) {
        if (!massRepository.existsById(mass.getId())) {
            throw new ResourceNotFoundException("resource not found");
        }
        return massRepository.save(mass);
    }

    private List<Mass> filterOutOnlyOnline(List<Mass> masses) {
        return masses.stream().filter(mass -> BooleanUtils.toBoolean(mass.getOnline()))
                .collect(Collectors.toList());
    }

    private List<Mass> filterByMassLang(List<Mass> masses, String massLang) {
        return masses.stream().filter(mass -> StringUtils.equals(mass.getLangCode(), massLang))
                .collect(Collectors.toList());
    }

    private List<Mass> filterOutRorateOnly(List<Mass> masses) {
        return masses.stream().filter(mass -> BooleanUtils.isTrue(mass.getRorate()))
                .collect(Collectors.toList());
    }

    public Optional<Mass> findScheduleConflict(Mass mass) {
        if (mass.isDeleted()) {
            return Optional.empty();
        }
        var rule = massCalendarMapper.toScheduleRule(mass.getCalendar());
        return massRepository.findByParishId(mass.getParishId()).stream()
                .filter(other -> !other.isDeleted() && !Objects.equals(mass.getId(), other.getId()))
                .filter(other -> ScheduleConflict.intersects(rule, massCalendarMapper.toScheduleRule(other.getCalendar())))
                .findFirst();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "#p0.id"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId")
    })
    public synchronized void removeMass(Mass mass){
        massRepository.delete(mass);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "#p0"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #result.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #result.parishId")
    })
    public synchronized Mass removeMass(String id) {
        Mass mass = requireMassForUpdate(id);
        massRepository.delete(mass);
        return mass;
    }

    public synchronized Triple<String, String, String> removeMass(String id, Instant from, Instant toExclusive) {
        Mass baseMass = requireMassForUpdate(id);
        try {
            return removeMassInterval(baseMass, from, toExclusive);
        } finally {
            // A split can persist the first remainder before the second write fails.
            var cache = cacheManager.getCache("massCache");
            if (cache != null) {
                cache.evictIfPresent(baseMass.getId());
                cache.evictIfPresent("massesByCity:" + baseMass.getCityId());
                cache.evictIfPresent("massesByParish:" + baseMass.getParishId());
            }
        }
    }

    private Triple<String, String, String> removeMassInterval(Mass baseMass, Instant from, Instant toExclusive) {
        var rule = massCalendarMapper.toScheduleRule(baseMass.getCalendar());
        Instant start = rule.start().toInstant();
        Instant end = rule.until();
        if ((end != null && end.isBefore(from)) || (toExclusive != null && !start.isBefore(toExclusive))) {
            return Triple.of(null, null, null);
        }

        Instant nextStart = toExclusive == null ? null : rule.starts(toExclusive,
                toExclusive.plus(7, java.time.temporal.ChronoUnit.DAYS)).findFirst().orElse(null);
        var calendar = baseMass.getCalendar();
        String updatedId = null;
        if (start.isBefore(from)) {
            Instant cutoff = from.minusSeconds(1);
            var truncated = new CalendarSchedule(start, calendar.rrule(), calendar.exdates()).withUntil(cutoff);
            baseMass.setCalendar(new Mass.CalendarDescription(truncated.dtstart(), truncated.rrule(), truncated.exdates()));
            updatedId = massRepository.save(baseMass).getId();
        }

        if (nextStart != null) {
            Mass remainder = updatedId == null ? baseMass : new Mass(baseMass);
            remainder.setCalendar(new Mass.CalendarDescription(nextStart, calendar.rrule()));
            String remainderId = massRepository.save(remainder).getId();
            return updatedId == null ? Triple.of(remainderId, null, null) : Triple.of(updatedId, remainderId, null);
        }
        if (updatedId == null) {
            massRepository.delete(baseMass);
            return Triple.of(null, null, baseMass.getId());
        }
        return Triple.of(updatedId, null, null);
    }


    @CacheEvict(cacheNames = "massCache", allEntries = true)
    public synchronized List<Mass> removeMasses(String parishId){
        return massRepository.deleteByParishId(parishId);
    }

    public List<MassDay> parishWeek(String parishId, LocalDate startDate) {
        MassSchedule schedule = scheduleFactory.build(getMassByParish(parishId), startDate);
        schedule.createSchedule();
        return schedule.getSchedule();
    }

    public MassSchedule weekMasses(String cityId, String parishId, LocalDate startDate,
                                   boolean online, boolean rorate, String massLang, boolean showPending) {
        List<Mass> masses;
        if (StringUtils.isNotEmpty(parishId)) {
            var parish = parishService.getParish(parishId)
                    .filter(value -> showPending || value.getState() == Parish.State.APPROVED);
            if (parish.isPresent()) {
                cityId = parish.get().getCityId();
                masses = getMassByParish(parishId);
            } else {
                masses = List.of();
            }
        } else {
            masses = getMassByCity(cityId);
            if (!showPending) {
                var notApproved = parishService.getNotApprovedParishIds(cityId);
                masses = masses.stream().filter(mass -> !notApproved.contains(mass.getParishId())).toList();
            }
        }
        if (online) {
            masses = filterOutOnlyOnline(masses);
        }
        if (StringUtils.isNotEmpty(massLang)) {
            masses = filterByMassLang(masses, massLang);
        }
        if (rorate) {
            masses = filterOutRorateOnly(masses);
        }

        MassSchedule schedule = scheduleFactory.build(masses, startDate);
        schedule.createSchedule();
        schedule.setNav(buildMassNavigation(schedule, cityId, parishId,
                Boolean.toString(online), massLang, rorate));
        return schedule;
    }

    public MassNav buildMassNavigationByFacetDistribution(final String cityId, final String parishId,
                                                                                   final boolean online, final String massLang,
                                                                                   final boolean rorate,
                                                          final Map<String, Map<String, Integer>> facetDistribution) {
        //--- Построение selected на основании переданных фильтров
        final TreeMap<String, MassFilterValue> selectedValuesMap = new TreeMap<>();

        if (StringUtils.isNotEmpty(cityId)) {
            selectedValuesMap.put(MassFilterType.CITY.getName(), MassFilterValue.builder().type(MassFilterType.CITY)
                    .name(MassFilterType.CITY.getName()).value(cityId).build());
        }
        if (StringUtils.isNotEmpty(parishId)) {
            selectedValuesMap.put(MassFilterType.PARISH.getName(), MassFilterValue.builder().type(MassFilterType.PARISH)
                    .name(MassFilterType.PARISH.getName()).value(parishId).build());
        }
        selectedValuesMap.put(MassFilterType.ONLINE.getName(), MassFilterValue.builder().type(MassFilterType.ONLINE)
                .name(MassFilterType.ONLINE.getName()).value(Boolean.toString(online)).build());
        if (StringUtils.isNotEmpty(massLang)) {
            selectedValuesMap.put(MassFilterType.LANG.getName(), MassFilterValue.builder().type(MassFilterType.LANG)
                    .name(MassFilterType.LANG.getName()).value(massLang).build());
        }
        if (rorate) {
            selectedValuesMap.put(MassFilterType.RORATE.getName(), MassFilterValue.builder().type(MassFilterType.RORATE)
                    .name(MassFilterType.RORATE.getName()).value(String.valueOf(rorate)).build());
        }

        //--- Построение guided на основании полученных из индекса facet distribution
        // FIXME и запросов(кэшированных) , тут можно заменить на запросы (2 запроса) facet distribution
        //  по городам (1) и парафиям города (2)
        final Set<MassFilterValue> massFilterValues = new HashSet<>();

        facetDistribution.getOrDefault(SearchRecord.FilterableAttribute.ONLINE, Collections.emptyMap())
                .keySet()
                .forEach(value ->
                        massFilterValues.add(MassFilterValue.builder()
                                .type(MassFilterType.ONLINE)
                                .name(value)
                                .value(value)
                                .build())
                );

        facetDistribution.getOrDefault(SearchRecord.FilterableAttribute.LANG, Collections.emptyMap())
                .keySet()
                .forEach(value ->
                        massFilterValues.add(MassFilterValue.builder()
                                .type(MassFilterType.LANG)
                                .name(value)
                                .value(value)
                                .build())
                );

        facetDistribution.getOrDefault(SearchRecord.FilterableAttribute.RORATE, Collections.emptyMap())
                .keySet()
                .forEach(value ->
                        massFilterValues.add(MassFilterValue.builder()
                                .type(MassFilterType.RORATE)
                                .name(value)
                                .value(value)
                                .build())
                );

        //TODO здесь не facet`ы , а отдельный запрос
        //всегда должны быть доступны все парафии выбранного города (а не только одна выбранная)
        final List<MassFilterValue> parishMassFilterValues = getAllApprovedParishesByCityAsMassFilterValues(cityId);
        massFilterValues.addAll(parishMassFilterValues);

        // TODO to consider contry or other region (no need to return all cities in the system)

        List<City> cities = cityService.getAllCities();
        Set<String> cityWithApprovedParishesIds = cityService.getCityWithApprovedParishesIds();

        final String locale = getUserLocale().orElse(null);

        //TODO здесь не facet`ы , а отдельный запрос
        List<MassFilterValue> cityFilterValues = cities.stream()
                .filter(city -> cityWithApprovedParishesIds.contains(city.getId()))
                .map(city -> MassFilterValue.builder()
                        .name(getLocalizedCityName(city, locale))
                        .value(city.getId())
                        .type(MassFilterType.CITY)
                        .build())
                .toList();
        massFilterValues.addAll(cityFilterValues);

        //сортируем значения на основании локали пользователя
        final Comparator<MassFilterValue> comparator = COMPARATOR_CACHE.computeIfAbsent(locale, key -> {
            final Collator collator = Collator.getInstance(new Locale(key));
            return (first, second) -> collator.compare(first.getName().toLowerCase(), second.getName().toLowerCase());
        });

        final TreeMap<String, Set<MassFilterValue>> guidedValuesMap = massFilterValues.stream().collect(
                Collectors.groupingBy(
                        massFilterValue -> massFilterValue.getType().getName(),
                        TreeMap::new,
                        Collectors.toCollection(() -> new TreeSet<>(comparator))
                )
        );

        return new MassNav(selectedValuesMap, guidedValuesMap);
    }

    public MassNav buildMassNavigation(MassSchedule massSchedule, String cityId,
                                       String parishId, String online, String massLang,
                                       boolean rorate){

        MassNav nav = new MassNav();
        Collection<Map<LocalTime, List<MassInfo>>> massInfosByTime = massSchedule.getMassesByDay().values();

        Set<MassFilterValue> massFilterValues =  massInfosByTime.stream()
                .flatMap(x -> x.values().stream())
                .flatMap(y -> y.stream())
                .map(massInfo -> {
                    //значения флага online определяются только на основе парафий, попавших в выборку
                    // (а выборка, как минимум, по городу есть всегда)
                    MassFilterValue onlineFilterValue = MassFilterValue.builder()
                            .type(MassFilterType.ONLINE)
                            .name(String.valueOf(massInfo.isOnline()))
                            .value(String.valueOf(massInfo.isOnline()))
                            .build();
                    //значения lang определяются только на основе парафий, попавших в выборку
                    // (а выборка, как минимум, по городу есть всегда)
                    MassFilterValue langFilterValue = MassFilterValue.builder()
                            .type(MassFilterType.LANG)
                            .name(massInfo.getLangCode())
                            .value(massInfo.getLangCode())
                            .build();
                    //значения rorate определяются только на основе парафий, попавших в выборку
                    // (а выборка, как минимум, по городу есть всегда)
                    MassFilterValue rorateFilterValue = MassFilterValue.builder()
                            .type(MassFilterType.RORATE)
                            .name(String.valueOf(massInfo.isRorate()))
                            .value(String.valueOf(massInfo.isRorate()))
                            .build();
                    return  Arrays.asList(onlineFilterValue, langFilterValue, rorateFilterValue);
                })
                .flatMap(filterValues -> filterValues.stream())
                .collect(Collectors.toSet());

        //всегда должны быть доступны все парафии выбранного города (а не только одна выбранная)
        final List<MassFilterValue> parishMassFilterValues = getAllApprovedParishesByCityAsMassFilterValues(cityId);
        massFilterValues.addAll(parishMassFilterValues);

            // TODO to consider contry or other region (no need to return all cities in the system)

        List<City> cities = cityService.getAllCities();
        Set<String> cityWithApprovedParishesIds = cityService.getCityWithApprovedParishesIds();

        final String locale = getUserLocale().orElse(null);

        List<MassFilterValue> cityFilterValues = cities.stream()
                .filter(city -> cityWithApprovedParishesIds.contains(city.getId()))
                .map(city -> MassFilterValue.builder()
                        .name(getLocalizedCityName(city, locale))
                        .value(city.getId())
                        .type(MassFilterType.CITY)
                        .build())
                .collect(Collectors.toList());
        massFilterValues.addAll(cityFilterValues);

        //сортируем значения на основании локали пользователя
        final Comparator<MassFilterValue> comparator = COMPARATOR_CACHE.computeIfAbsent(locale, key -> {
            final Collator collator = Collator.getInstance(new Locale(key));
            return (first, second) -> collator.compare(first.getName().toLowerCase(), second.getName().toLowerCase());
        });

        final TreeMap<String, Set<MassFilterValue>> guidedMap = massFilterValues.stream().collect(
                Collectors.groupingBy(
                        massFilterValue -> massFilterValue.getType().getName(),
                        TreeMap::new,
                        Collectors.toCollection(() -> new TreeSet<>(comparator))
                )
        );
        nav.setGuided(guidedMap);
        TreeMap<String, MassFilterValue> selectedMap = new TreeMap<>();
        if(StringUtils.isNotEmpty(cityId)){
            selectedMap.put(MassFilterType.CITY.getName(), MassFilterValue.builder().type(MassFilterType.CITY)
                .name(MassFilterType.CITY.getName()).value(cityId).build());
        }
        if(StringUtils.isNotEmpty(parishId)){
            selectedMap.put(MassFilterType.PARISH.getName(), MassFilterValue.builder().type(MassFilterType.PARISH)
                .name(MassFilterType.PARISH.getName()).value(parishId).build());
        }
        if(StringUtils.isNotEmpty(online)){
            selectedMap.put(MassFilterType.ONLINE.getName(), MassFilterValue.builder().type(MassFilterType.ONLINE)
                .name(MassFilterType.ONLINE.getName()).value(online).build());
        }
        if(StringUtils.isNotEmpty(massLang)){
            selectedMap.put(MassFilterType.LANG.getName(), MassFilterValue.builder().type(MassFilterType.LANG)
                .name(MassFilterType.LANG.getName()).value(massLang).build());
        }
        if(rorate){
            selectedMap.put(MassFilterType.RORATE.getName(), MassFilterValue.builder().type(MassFilterType.RORATE)
                    .name(MassFilterType.RORATE.getName()).value(String.valueOf(rorate)).build());
        }
        nav.setSelected(selectedMap);
        return nav;
    }

    /**
     * Получить все подтвержденные парафии города в виде {@link MassFilterValue}
     */
    private List<MassFilterValue> getAllApprovedParishesByCityAsMassFilterValues(final String cityId) {
        final String locale = getUserLocale().orElse(null);

        return parishRepository.findByCityIdAndState(cityId, Parish.State.APPROVED).stream()
                .map(parish -> MassFilterValue.builder()
                        .type(MassFilterType.PARISH)
                        .name(Optional.ofNullable(getLocalizedParishShortName(parish, locale))
                                .orElseGet(() -> getLocalizedParishName(parish, locale)))
                        .value(parish.getId())
                        .build())
                .collect(Collectors.toList());
    }
}
