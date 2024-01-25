package by.imsha.service;

import by.imsha.domain.Mass;
import by.imsha.domain.dto.MassInfo;
import by.imsha.domain.dto.MassParishInfo;
import by.imsha.domain.dto.MassSchedule;
import by.imsha.domain.dto.mapper.MassParishInfoMapper;
import by.imsha.meilisearch.model.SearchResultItem;
import by.imsha.utils.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alena Misan
 * TODO refactor process of schedule initialization
 */
@Service
public class ScheduleFactory {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());


    @Autowired
    private ParishService parishService;

    private MassSchedule build(List<Mass> masses, LocalDate startDate, LocalDate endDate) {
        MassSchedule schedule =  new MassSchedule(startDate);
        for (Mass mass : masses) {
            long singleStartTimestamp = mass.getSingleStartTimestamp();
            // TODO get ZONE from parish : to support
            LocalDateTime singleStartTime = singleStartTimestamp > 0 ? ServiceUtils.timestampToLocalDate(singleStartTimestamp, ServiceUtils.BEL_ZONE_ID) : null;


            if (mass.getParishId() == null) {
                log.error(String.format("Mass with ID: %s is configured incorrectly - parish ID can not be null!", mass.getId()));
                continue;
            }else if(parishService.getParish(mass.getParishId()) == null){
                log.error(String.format("Mass with ID: %s is configured incorrectly - not existing parish is configured : %s!", mass.getId(), mass.getParishId()));
                continue;
            }
            if (singleStartTime != null) {
                if (mass.getTime() != null) {
                    log.error(String.format("Mass with ID: %s is configured incorrectly - time and singleStartTime cannot be not null at the same time!", mass.getId()));
                    continue;
                }
                LocalDate singleStartDate = singleStartTime.toLocalDate();

                if (singleStartDate.isAfter(LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth()).minusDays(1)) && singleStartDate.isBefore(endDate)) {
                    schedule.populateContainers(mass, singleStartTime.getDayOfWeek(), singleStartTime.toLocalTime());
                }
            } else {
                if (mass.getTime() == null) {
                    log.error(String.format("Mass with ID: %s is configured incorrectly - time and singleStartTime cannot be null at the same time!", mass.getId()));
                    continue;
                }
                int[] days = mass.getDays();
                for (int day : days) {
                    schedule.populateContainers(mass, DayOfWeek.of(day), LocalTime.parse(mass.getTime()));
                }
            }
        }
        return schedule;
    }


    /**
     * Build schedule based on week-day, filters masses that is configured not in week from provided startWeekDay
     *
     * */
    public MassSchedule build(List<Mass> masses, LocalDate startDate) {
        LocalDate endDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth()).plusDays(7);
        return build(masses, startDate, endDate);

    }

    public MassSchedule build(final LocalDate dateFrom, final List<SearchResultItem> resultItems) {
        final MassSchedule massSchedule = new MassSchedule(dateFrom, true);

        final Map<String, MassParishInfo> parishInfoCache = new HashMap<>();

        resultItems.forEach(resultItem -> {
            final DayOfWeek dayOfWeek = resultItem.date().getDayOfWeek();

            final MassInfo massInfo = new MassInfo();
            massInfo.setId(resultItem.massId());
            massInfo.setLangCode(resultItem.lang());
            massInfo.setParish(
                    parishInfoCache.computeIfAbsent(
                            resultItem.parish().id(),
                            parishId -> parishService.getParish(parishId)
                                    .map(MassParishInfoMapper.MAPPER::toMassParishInfo)
                                    .orElse(null))
            );
            massInfo.setDuration(resultItem.duration());
            massInfo.setInfo(resultItem.notes());
            massInfo.setDays(new int[]{dayOfWeek.getValue()});//берем день недели из текущей date
            massInfo.setOnline(resultItem.online());
            massInfo.setRorate(resultItem.rorate());
            massInfo.setLastModifiedDate(resultItem.lastModifiedDate());

            massSchedule.populateContainers(massInfo, dayOfWeek, resultItem.time());
        });

        return massSchedule;
    }
}
