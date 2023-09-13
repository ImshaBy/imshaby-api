package by.imsha.service;

import by.imsha.domain.City;
import by.imsha.domain.Mass;
import by.imsha.domain.dto.*;
import by.imsha.domain.dto.mapper.MassInfoMapper;
import by.imsha.repository.MassRepository;
import by.imsha.utils.ServiceUtils;
import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import com.github.rutledgepaulv.rqe.pipes.QueryConversionPipeline;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
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

import javax.annotation.PostConstruct;
import java.text.Collator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static by.imsha.utils.Constants.*;

/**
 * @author Alena Misan
 */

@Service
public class MassService {
    private static Logger logger = LoggerFactory.getLogger(MassService.class);
    /**
     * Мап, для кэширования компараторов для различных локалей
     */
    private static final Map<String, Comparator<MassFilterValue>> COMPARATOR_CACHE = new HashMap<>();

    private QueryConversionPipeline pipeline = QueryConversionPipeline.defaultPipeline();

    private MongoVisitor mongoVisitor = new MongoVisitor();

    private static MassService INSTANCE;

    private static final int WEEK_DAYS_COUNT = (int) ChronoUnit.WEEKS.getDuration().toDays();

    @PostConstruct
    public void initInstance(){
        INSTANCE = this;
    }

    @Autowired
    private MassRepository massRepository;

    @Autowired
    private CityService cityService;

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId"),
            @CacheEvict(cacheNames = "massCache", key = "'oldestMass:' + #p0.parishId")
    })
    public Mass createMass(Mass mass){
        return massRepository.save(mass);
    }

    public List<Mass> createMassesWithList(List<Mass> masses){
        return massRepository.saveAll(masses);
    }

    public static boolean isPeriodicMass(Mass mass) {
        long singleStartTimestamp = mass.getSingleStartTimestamp();
        return singleStartTimestamp == 0;
    }

    public static boolean isMassTimeConfigIsValid(Mass mass) {
        long singleStartTimestamp = mass.getSingleStartTimestamp();
        String time = mass.getTime();
        boolean timeIsNotNull = singleStartTimestamp == 0 && StringUtils.isNotBlank(time);
        boolean singleTimestampIsNotNull = singleStartTimestamp > 0 && StringUtils.isBlank(time);
        return timeIsNotNull || singleTimestampIsNotNull;
    }

    public static boolean isScheduleMassDaysIsNotEmpty(Mass mass) {
        int[] days = mass.getDays();
        boolean validScheduledMass = true;
        if (isPeriodicMass(mass)) {
            validScheduledMass = ArrayUtils.isNotEmpty(days);
        }
        return validScheduledMass;
    }
    public static boolean isCorrectEndDateForPeriodicMass(Mass mass) {
        int[] days = mass.getDays();
        boolean validScheduledMass = true;
        if (isPeriodicMass(mass)) {
            validScheduledMass = ArrayUtils.isNotEmpty(days);
        }
        return validScheduledMass;
    }

    public static boolean isScheduleMassDaysAreCorrect(Mass mass) {
        if (isPeriodicMass(mass) && isScheduleMassDaysIsNotEmpty(mass)) {
            for (int day : mass.getDays()) {
                if (day < 1 || day > WEEK_DAYS_COUNT) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isScheduleMassStartEndDatesAreCorrect(Mass mass) {
        if (isPeriodicMass(mass)) {
            LocalDate startDate = mass.getStartDate();
            LocalDate endDate = mass.getEndDate();
            return startDate == null || endDate == null || !startDate.isAfter(endDate);
        }
        return true;
    }

    public static boolean isScheduleMassTimeIsNotBlank(Mass mass) {
        String time = mass.getTime();
        boolean validScheduledMass = true;
        if (isPeriodicMass(mass)) {
            validScheduledMass = StringUtils.isNotBlank(time);
        }
        return validScheduledMass;
    }

    public static boolean isScheduleMassDaysInDatePeriod(Mass mass) {
        if (isPeriodicMass(mass) && isMassTimeConfigIsValid(mass) && isScheduleMassDaysIsNotEmpty(mass)
            && isScheduleMassDaysAreCorrect(mass) && isScheduleMassStartEndDatesAreCorrect(mass)) {
            return mass.getDays().length == buildValidWeekDaysInDatePeriod(mass).length;
        }
        return true;
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

    public static boolean isUniqueMassTime(Mass mass) {
        if (isMassTimeConfigIsValid(mass) && isScheduleMassTimeIsNotBlank(mass)
            && isScheduleMassDaysIsNotEmpty(mass) && isScheduleMassDaysAreCorrect(mass)
            && isScheduleMassStartEndDatesAreCorrect(mass) && isScheduleMassDaysInDatePeriod(mass)) {
            boolean[] commDays = new boolean[WEEK_DAYS_COUNT], daysToCheck = new boolean[WEEK_DAYS_COUNT];
            LocalDate commStartDate, commEndDate, date1, date2;
            Mass massToCheck = mass.asPeriodic();
            Arrays.stream(massToCheck.getDays()).forEach(day -> daysToCheck[day - 1] = true);
            List<Mass> masses = INSTANCE.getMassByParish(mass.getParishId());
            for (Mass massP : masses) {

                if (massP.getId().equals(mass.getId()) || massP.isDeleted()) {
                    continue;
                }
                massP = massP.asPeriodic();
                if (!massP.getTime().equals(massToCheck.getTime())) {
                    continue;
                }
                date1 = massP.getStartDate() == null ? ServiceUtils.timestampToLocalDate(0L).toLocalDate() : massP.getStartDate();
                date2 = massToCheck.getStartDate() == null ? ServiceUtils.timestampToLocalDate(0L).toLocalDate() : massToCheck.getStartDate();
                if (date1.isAfter(date2)) {
                    commStartDate = date1;
                } else {
                    commStartDate = date2;
                }
                date1 = massP.getEndDate() == null ? commStartDate.plusWeeks(1) : massP.getEndDate();
                date2 = massToCheck.getEndDate() == null ? commStartDate.plusWeeks(1) : massToCheck.getEndDate();
                if (date1.isBefore(date2)) {
                    commEndDate = date1;
                } else {
                    commEndDate = date2;
                }
                if (commStartDate.isAfter(commEndDate)) {
                    continue;
                }
                Arrays.fill(commDays, false);
                Arrays.stream(massP.getDays()).forEach(day -> commDays[day - 1] = daysToCheck[day - 1]);
                for (int day = 0; (commStartDate.isBefore(commEndDate) || commStartDate.isEqual(commEndDate))
                    && day < WEEK_DAYS_COUNT; day++) {
                    if (commDays[commStartDate.getDayOfWeek().getValue() - 1]) {
                        if(logger.isErrorEnabled()){
//                            logger.error(String.format("Mass (time = %s, startDate = %s, endDate =%s, days = %s) has issues with isUniqueMassTime verification due to mass with id = %s (time = %s, startDate = %s, endDate = %s, days = %s)", mass.getTime(), mass.getStartDate(), mass.getEndDate(), Arrays.toString(mass.getDays()), massP.getId()),
//                                    massP.getTime(), massP.getStartDate(), massP.getEndDate(), Arrays.toString(massP.getDays()));
                            logger.error(String.format("Mass = %s has issues with isUniqueMassTime verification due to mass  = %s", massToString(mass), massToString(massP)));
                        }
                        return false;
                    }
                    commStartDate = commStartDate.plusDays(1);
                }
            }
        }
        return true;
    }

    private static String massToString(Mass mass){
            return new ToStringBuilder(mass)
                    .append("id", mass.getId())
                    .append("cityId", mass.getCityId())
                    .append("time", mass.getTime())
                    .append("days", mass.getDays())
                    .append("online", mass.getOnline())
                    .append("rorate", mass.getRorate())
                    .append("parishId", mass.getParishId())
                    .append("deleted", mass.isDeleted())
                    .append("notes", mass.getNotes())
                    .append("singleStartTimestamp", mass.getSingleStartTimestamp())
                    .append("startDate", mass.getStartDate())
                    .append("endDate", mass.getEndDate())
                    .toString();
    };

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


    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "#p1.id"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p1.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p1.parishId"),
            @CacheEvict(cacheNames = "massCache", key = "'oldestMass:' + #p1.parishId")
    })
    public Mass updateMass(UpdateMassInfo massInfo, Mass massToUpdate){
        MassInfoMapper.MAPPER.updateMassFromDTO(massInfo, massToUpdate);
        return massRepository.save(massToUpdate);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "#p0.id"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId"),
            @CacheEvict(cacheNames = "massCache", key = "'oldestMass:' + #p0.parishId")
    })
    public Mass updateMass(Mass mass){
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
                                       String parishId, String online, String massLang){
        MassNav nav = new MassNav();
        Collection<Map<LocalTime, List<MassInfo>>> massInfosByTime = massSchedule.getMassesByDay().values();

        Set<MassFilterValue> massFilterValues =  massInfosByTime.stream()
                .flatMap(x -> x.values().stream())
                .flatMap(y -> y.stream())
                .map(massInfo -> {
                    MassFilterValue parishFilterValue = MassFilterValue.builder()
                            .type(MassFilterType.PARISH)
                            .name(Optional.ofNullable(massInfo.getParish().getShortName())
                                    .orElseGet(massInfo.getParish()::getName))
                            .value(massInfo.getParish().getParishId())
                            .build();

                    MassFilterValue onlineFilterValue = MassFilterValue.builder()
                            .type(MassFilterType.ONLINE)
                            .name(ONLINE_FILTER)
                            .value(String.valueOf(massInfo.isOnline()))
                            .build();

                    MassFilterValue langFilterValue = MassFilterValue.builder()
                            .type(MassFilterType.LANG)
                            .name(massInfo.getLangCode())
                            .value(massInfo.getLangCode())
                            .build();
                    return  Arrays.asList(parishFilterValue, onlineFilterValue, langFilterValue);
                })
                .flatMap(filterValues -> filterValues.stream())
                .collect(Collectors.toSet());

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
        nav.setSelected(selectedMap);
        return nav;
    }

    public static LocalDateTime getOldestModifiedMassTimeForParish(String parishId){
        Mass oldestModifiedMass = INSTANCE.findOldestModifiedMass(parishId);
        return oldestModifiedMass != null ? oldestModifiedMass.getLastModifiedDate() : null;
    }
}
