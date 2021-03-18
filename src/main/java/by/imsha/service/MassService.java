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

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static by.imsha.utils.Constants.*;

/**
 * @author Alena Misan
 */

@Service
public class MassService {
    private static Logger logger = LoggerFactory.getLogger(MassService.class);

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

    @Caching(evict = {
            @CacheEvict(cacheNames = "massCache", key = "'massesByCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "massCache", key = "'massesByParish:' + #p0.parishId"),
            @CacheEvict(cacheNames = "massCache", key = "'oldestMass:' + #p0.parishId")
    })
    public Mass createMass(Mass mass){
        return massRepository.save(mass);
    }

    public List<Mass> createMassesWithList(List<Mass> masses){
        return massRepository.save(masses);
    }

    public static boolean isMassTimeConfigIsValid(Mass mass) {
        long singleStartTimestamp = mass.getSingleStartTimestamp();
        String time = mass.getTime();
        boolean timeIsNotNull = StringUtils.isNotBlank(time) && singleStartTimestamp == 0;
        boolean singleTimestampIsNotNull = singleStartTimestamp != 0 && StringUtils.isBlank(time);
        return timeIsNotNull || singleTimestampIsNotNull;
    }


    public static boolean isScheduleMassDaysIsCorrect(Mass mass) {
        String time = mass.getTime();
        int[] days = mass.getDays();
        boolean validScheduledMass = true;
        if (StringUtils.isNotBlank(time)) {
            validScheduledMass = ArrayUtils.isNotEmpty(days);
        }
        return validScheduledMass;
    }

    public static boolean isScheduleMassTimeIsCorrect(Mass mass) {
        String time = mass.getTime();
        int[] days = mass.getDays();
        boolean validScheduledMass = true;
        if (ArrayUtils.isNotEmpty(days)) {
            validScheduledMass = StringUtils.isNotBlank(time);
        }
        return validScheduledMass;
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
    public Mass getMass(String id){
        return massRepository.findOne(id);
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
        return masses.stream().filter(mass -> mass.getOnline())
                .collect(Collectors.toList());
    }

    public List<Mass> filterByMassLang(List<Mass> masses, String massLang){
        return masses.stream().filter(mass -> mass.getLangCode().equals(massLang))
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
        massRepository.delete(mass.getId());
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
        boolean updated = false;
        Mass massToSave = baseMass;
        String updatedMassId = null, createdMassId = null, removedMassId = null;
        if ((baseStartDate == null || fromDate.isAfter(baseStartDate))
                && (baseEndDate == null || !baseEndDate.isBefore(fromDate))) {
            massToSave.setEndDate(fromDate.minusDays(1));
            massRepository.save(massToSave);
            updatedMassId = massToSave.getId();
            updated = true;
        }
        if (toDate != null && (baseEndDate == null || baseEndDate.isAfter(toDate))
                && (baseStartDate == null || !baseStartDate.isAfter(toDate))) {
            if (updated) {
                massToSave = new Mass(baseMass);
            }
            massToSave.setStartDate(toDate.plusDays(1));
            massToSave.setEndDate(baseEndDate);
            massToSave = massRepository.save(massToSave);
            if (updated) {
                createdMassId = massToSave.getId();
            } else {
                updatedMassId = massToSave.getId();
            }
        }
        if (baseStartDate != null && !fromDate.isAfter(baseStartDate)
                && (toDate == null || baseEndDate != null && !toDate.isBefore(baseEndDate))) {
            removeMass(baseMass);
            removedMassId = baseMass.getId();
        }
        return Triple.of(updatedMassId, createdMassId, removedMassId);
    }

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
                            .name(massInfo.getParish().getName())
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
        List<MassFilterValue> cityFilterValues = cities.stream()
                .map(city -> MassFilterValue.builder()
                        .name(city.getName())
                        .value(city.getId())
                        .type(MassFilterType.CITY)
                        .build())
                .collect(Collectors.toList());
        massFilterValues.addAll(cityFilterValues);

        TreeMap<String, Set<MassFilterValue>> guidedMap = new TreeMap();
        massFilterValues.forEach(
                massFilterValue -> guidedMap.computeIfAbsent(massFilterValue.getType().getName(), value -> new HashSet<MassFilterValue>())
                                    .add(massFilterValue)
        );
        nav.setGuided(guidedMap);
        TreeMap<String, MassFilterValue> selectedMap = new TreeMap();
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
