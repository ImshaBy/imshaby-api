package by.imsha.service;

import by.imsha.domain.City;
import by.imsha.domain.Mass;
import by.imsha.domain.dto.MassDay;
import by.imsha.domain.dto.MassInfo;
import by.imsha.domain.dto.MassSchedule;
import by.imsha.meilisearch.model.Parish;
import by.imsha.meilisearch.model.SearchRecord;
import by.imsha.meilisearch.writer.MeilisearchWriter;
import by.imsha.utils.DateTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MassIndexService {

    private final DateTimeProvider dateTimeProvider;
    private final ScheduleFactory scheduleFactory;
    private final MassService massService;
    private final CityService cityService;
    private final MeilisearchWriter meilisearchWriter;

    public void updateIndex() {
        final List<SearchRecord> searchRecords = new LinkedList<>();
        final LocalDate today = dateTimeProvider.today();
        // вчера (-1 день)
        final LocalDate startDate = today.minusDays(1);
        // сегодня + неделя (6 дней текущей недели) + ещё 1 день
        final LocalDate endDate = today.plusDays(7);

        for (City city : cityService.getAllCities()) {
            List<Mass> masses = massService.getMassByCity(city.getId());
            MassSchedule massHolder = scheduleFactory.build(masses, startDate, endDate);
            massHolder.createSchedule(9);//9 дней (вчера + текущая неделя + ещё 1 день)
            List<MassDay> schedule = massHolder.getSchedule();

            for (MassDay massDay : schedule) {
                for (MassDay.MassHour massHour : massDay.getMassHours()) {
                    for (MassInfo massInfo : massHour.getData()) {
                        searchRecords.add(
                                SearchRecord.builder()
                                        .recordId(UUID.randomUUID().toString())
                                        .massId(massInfo.getId())
                                        .duration(90)
                                        .time(massHour.getHour())
                                        .date(massDay.getDate())
                                        .parish(
                                                Parish.builder()
                                                        .id(massInfo.getParish().getParishId())
                                                        .build()
                                        )
                                        .notes(massInfo.getInfo())
                                        .lang(massInfo.getLangCode())
                                        .online(massInfo.isOnline())
                                        .rorate(massInfo.isRorate())
                                        .city(
                                                by.imsha.meilisearch.model.City.builder()
                                                        .id(city.getId())
                                                        .build()
                                        )
                                        .build()
                        );
                    }
                }
            }
        }

        meilisearchWriter.refreshAllData(searchRecords);
    }
}
