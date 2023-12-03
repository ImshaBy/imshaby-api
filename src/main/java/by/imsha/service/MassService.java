package by.imsha.service;

import by.imsha.domain.City;
import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.domain.dto.MassFilterType;
import by.imsha.domain.dto.MassFilterValue;
import by.imsha.domain.dto.MassInfo;
import by.imsha.domain.dto.MassNav;
import by.imsha.domain.dto.MassSchedule;
import by.imsha.repository.MassRepository;
import by.imsha.repository.ParishRepository;
import by.imsha.utils.ServiceUtils;
import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import com.github.rutledgepaulv.rqe.pipes.QueryConversionPipeline;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static by.imsha.utils.Constants.LIMIT;
import static by.imsha.utils.Constants.PAGE;
import static by.imsha.utils.Constants.WEEK_DAYS_COUNT;

/**
 * @author Alena Misan
 */

@Service
@Validated
public class MassService {
    private static Logger logger = LoggerFactory.getLogger(MassService.class);
    /**
     * Мап, для кэширования компараторов для различных локалей
     */
    private static final Map<String, Comparator<MassFilterValue>> COMPARATOR_CACHE = new HashMap<>();

    private QueryConversionPipeline pipeline = QueryConversionPipeline.defaultPipeline();

    private MongoVisitor mongoVisitor = new MongoVisitor();

    private static MassService INSTANCE;

    @PostConstruct
    public void initInstance(){
        INSTANCE = this;
    }

    @Autowired
    private MassRepository massRepository;

    @Autowired
    private CityService cityService;

    @Autowired
    private ParishRepository parishRepository;

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId"),
            @CacheEvict(cacheNames = "massCache", key = "'oldestMass:' + #p0.parishId")
    })
    public Mass createMass(@Valid Mass mass){
        return massRepository.save(mass);
    }

    private static int[] buildValidWeekDaysInDatePeriod(Mass mass) {
        LocalDate startDate = mass.getStartDate();
        LocalDate endDate = mass.getEndDate();
        int[] baseDays = mass.getDays();
        if(startDate == null && endDate != null){
            startDate = LocalDate.now();
        }
        if (startDate == null || endDate == null) {
            return baseDays;
        }

        if (ChronoUnit.WEEKS.between(startDate, endDate) >= 1) {
            return baseDays;
        }
        int startDay = startDate.getDayOfWeek().getValue();
        int endDay = endDate.getDayOfWeek().getValue();
        boolean[] invalidWeekDays = new boolean[WEEK_DAYS_COUNT];
        int invalidDaysCount = 0;
        for (int day : baseDays) {
            if (startDay <= endDay && (day < startDay || day > endDay)
                || day > endDay && day < startDay) {
                invalidWeekDays[day - 1] = true;
                invalidDaysCount++;
            }
        }
        if (invalidDaysCount == 0) {
            return baseDays;
        }
        int[] validWeekDays = new int[baseDays.length - invalidDaysCount];
        int i = 0;
        for (int day : baseDays) {
            if (!invalidWeekDays[day - 1]) {
                validWeekDays[i++] = day;
            }
        }
        return validWeekDays;
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
        return massRepository.findById(id);
    }

    public List<Mass> search(String filter){
        // TODO can be added default sorting
        return search(filter, PAGE, LIMIT, null);
    }

    public List<Mass> search(String filter, int offset, int limit, String sort){
        Query query = prepareQuery(filter, offset, limit, sort);
        if (query == null) return null;
        List<Mass> masses = this.massRepository.search(query, Mass.class);
        return masses;
    }

    public Query prepareQuery(String filter, int offset, int limit, String sort) {
        if(StringUtils.isBlank(filter)){
            if(logger.isInfoEnabled()){
                logger.info("No searching masses: query is blank");
            }
            return null;
        }
        int[] offsetAndLimit = ServiceUtils.calculateOffsetAndLimit(offset, limit);
        Condition<GeneralQueryBuilder> condition = pipeline.apply(filter, Mass.class);
        return ServiceUtils.buildMongoQuery(sort, offsetAndLimit[0], offsetAndLimit[1], condition, mongoVisitor);
    }

    public List<Mass> filterOutOnlyOnline(List<Mass> masses){
        return masses.stream().filter(mass -> BooleanUtils.toBoolean(mass.getOnline()))
                .collect(Collectors.toList());
    }

    public List<Mass> filterByMassLang(List<Mass> masses, String massLang){
        return masses.stream().filter(mass -> StringUtils.equals(mass.getLangCode(), massLang))
            .collect(Collectors.toList());
    }

    public List<Mass> filterOutRorateOnly(List<Mass> masses){
        return masses.stream().filter(mass -> BooleanUtils.isTrue(mass.getRorate()))
                .collect(Collectors.toList());
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "#p0.id"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId"),
            @CacheEvict(cacheNames = "massCache", key = "'oldestMass:' + #p0.parishId")
    })
    public Mass updateMass(@Valid Mass mass){
        return massRepository.save(mass);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "#p0.id"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId"),
            @CacheEvict(cacheNames = "massCache", key = "'oldestMass:' + #p0.parishId")
    })
    public void removeMass(Mass mass){
        massRepository.delete(mass);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "#p0.id"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId"),
            @CacheEvict(cacheNames = "massCache", key = "'oldestMass:' + #p0.parishId")
    })
    public Triple<String, String, String> removeMass(Mass baseMass, LocalDate fromDate, LocalDate toDate){
        LocalDate baseStartDate = baseMass.getStartDate();
        LocalDate baseEndDate = baseMass.getEndDate();
        boolean updated = false, remove = false;
        Mass massToSave = baseMass;
        int[] validWeekDays, baseDays = baseMass.getDays();
        String updatedMassId = null, createdMassId = null, removedMassId = null;
        if ((baseStartDate == null || fromDate.isAfter(baseStartDate))
                && (baseEndDate == null || !baseEndDate.isBefore(fromDate))) {
            massToSave.setEndDate(fromDate.minusDays(1));
            validWeekDays = buildValidWeekDaysInDatePeriod(massToSave);
            if (validWeekDays.length > 0) {
                massToSave.setDays(validWeekDays);
                massRepository.save(massToSave);
                updatedMassId = massToSave.getId();
                updated = true;
            } else {
                remove = true;
            }
        }
        if (toDate != null && (baseEndDate == null || baseEndDate.isAfter(toDate))
                && (baseStartDate == null || !baseStartDate.isAfter(toDate))) {
            if (updated) {
                massToSave = new Mass(baseMass);
                massToSave.setDays(baseDays);
            } else {
                remove = false;
            }
            massToSave.setStartDate(toDate.plusDays(1));
            massToSave.setEndDate(baseEndDate);
            validWeekDays = buildValidWeekDaysInDatePeriod(massToSave);
            if (validWeekDays.length > 0) {
                massToSave.setDays(validWeekDays);
                massToSave = massRepository.save(massToSave);
                if (updated) {
                    createdMassId = massToSave.getId();
                } else {
                    updatedMassId = massToSave.getId();
                }
            } else if (massToSave.getId() != null) {
                remove = true;
            }
        }
        if (remove || baseStartDate != null && !fromDate.isAfter(baseStartDate)
                && (toDate == null || baseEndDate != null && !toDate.isBefore(baseEndDate))) {
            removeMass(baseMass);
            removedMassId = baseMass.getId();
        }
        return Triple.of(updatedMassId, createdMassId, removedMassId);
    }


    //TODO flush massCache by mass id
    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId"),
            @CacheEvict(cacheNames = "massCache", key = "'oldestMass:' + #p0.parishId")
    })
    public List<Mass> removeMasses(String parishId){
        return massRepository.deleteByParishId(parishId);
    }

    @Cacheable(cacheNames = "massCache", key = "'oldestMass:' +#p0")
    public Mass findOldestModifiedMass(String parishId){
        List<Mass> parishMasses = getMassByParish(parishId);
        Mass oldestMass = null;
        for (Mass parishMass : parishMasses) {
            if(oldestMass == null){
                oldestMass = parishMass;
            } else if (parishMass.getLastModifiedDate().isBefore(oldestMass.getLastModifiedDate())){
                oldestMass = parishMass;
            }
        }
        return oldestMass;
    }


    public MassNav buildMassNavigation(MassSchedule massSchedule, String cityId,
                                       String parishId, String online, String massLang,
                                       boolean rorate){

        if (massSchedule.getMassesByDay() == null || massSchedule.getMassesByDay().isEmpty()) {
            return MassNav.EMPTY_NAV;
        }

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

        List<MassFilterValue> cityFilterValues = cities.stream()
                .filter(city -> cityWithApprovedParishesIds.contains(city.getId()))
                .map(city -> MassFilterValue.builder()
                        .name(city.getLocalizedName())
                        .value(city.getId())
                        .type(MassFilterType.CITY)
                        .build())
                .collect(Collectors.toList());
        massFilterValues.addAll(cityFilterValues);

        //сортируем значения на основании локали пользователя
        final String locale = ServiceUtils.fetchUserLangFromHttpRequest();
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
        return parishRepository.findByCityIdAndState(cityId, Parish.State.APPROVED).stream()
                .map(parish -> MassFilterValue.builder()
                        .type(MassFilterType.PARISH)
                        .name(Optional.ofNullable(parish.getShortName())
                                .orElseGet(parish::getName))
                        .value(parish.getId())
                        .build())
                .collect(Collectors.toList());
    }

    public static LocalDateTime getOldestModifiedMassTimeForParish(String parishId){
        Mass oldestModifiedMass = INSTANCE.findOldestModifiedMass(parishId);
        return oldestModifiedMass != null ? oldestModifiedMass.getLastModifiedDate() : null;
    }
}
