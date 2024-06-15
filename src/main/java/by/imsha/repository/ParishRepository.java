package by.imsha.repository;

import by.imsha.domain.Parish;
import by.imsha.repository.projection.ParishExpirationInfo;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.mongodb.repository.Aggregation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface ParishRepository extends QuerableMongoRepository<Parish, String>{


    @Caching(evict = {
            @CacheEvict(cacheNames = "parishCache", key = "#p0.id"),
            @CacheEvict(cacheNames = "pendingParishes", key = "'parishCity:' + #p0.cityId"),
            @CacheEvict(cacheNames = "citiesWithParishCache", allEntries = true)
    })
    Parish save(Parish parish);


    @Caching(evict = {
            @CacheEvict(cacheNames = "parishCache"),
            @CacheEvict(cacheNames = "pendingParishes", allEntries = true),
            @CacheEvict(cacheNames = "citiesWithParishCache", allEntries = true)
    })
    void deleteParishById(String id);


    @Cacheable(cacheNames = "parishCache")
    Optional<Parish> findById(String id);

    Parish findByUserId(String userId);

    Optional<Parish> findParishByKey(String key);

    List<Parish> findByState(Parish.State state);

    List<Parish> findByCityIdAndState(String cityId, Parish.State state);

    /**
     * Получить информацию об устаревании расписаний парафий
     * <p>
     * Конвертация даты происходит с помощью {@link org.springframework.data.convert.Jsr310Converters.LocalDateTimeToDateConverter#convert(LocalDateTime)}
     * (как и для всех остальных полей в документах)
     * <p>
     * Алгоритм поиска:
     * 1) добавляем поле diffInMillis со значением разницы миллисекунд между checkDateTime и $lastConfirmRelevance
     * 2) добавляем поле group - со значениями EXPIRED (если diffInMillis <= 0), ALMOST (12 часов <= diffInMillis <= 36 часов), OTHER - все остальные
     *
     * @return информация об устаревающих и устаревших расписаниях парафий
     */
    @Aggregation(pipeline = {
            "{ $addFields: { diffInMillis: { $dateDiff: { startDate: ?0, endDate: { $dateAdd: { startDate: '$lastConfirmRelevance', unit: 'day', amount: '$updatePeriodInDays' } }, unit: 'millisecond' } } } }",
            "{ $addFields: { group: { $switch: { branches: [ { case: { $and: [ { $lte: [ '$diffInMillis', 129600000 ] }, { $gte: [ '$diffInMillis', 43200000 ] } ] }, then: 'ALMOST' }, { case: { $lte: ['$diffInMillis', 0] }, then: 'EXPIRED' } ], default: 'OTHER' } } } }",
            "{ $match: { group: { $ne: 'OTHER' } } }",
            "{ $group: { _id: null, " +
                    "expiredParishes: { $push: { $cond: [ { $eq: [ '$group', 'EXPIRED' ] }, { _id: '$_id', name: '$name', shortName: '$shortName', key: '$key', updatePeriodInDays: '$updatePeriodInDays', lastConfirmRelevance: '$lastConfirmRelevance' }, '$$REMOVE' ] } }, " +
                    "almostExpiredParishes: { $push: { $cond: [ { $eq: [ '$group', 'ALMOST' ] }, { _id: '$_id', name: '$name', shortName: '$shortName', key: '$key', updatePeriodInDays:'$updatePeriodInDays', lastConfirmRelevance:'$lastConfirmRelevance' }, '$$REMOVE' ] } } } }"
    })
    ParishExpirationInfo getParishExpirationData(LocalDateTime checkDateTime);
}
