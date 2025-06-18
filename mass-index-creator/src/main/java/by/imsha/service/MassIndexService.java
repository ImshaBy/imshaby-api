package by.imsha.service;

import by.imsha.domain.City;
import by.imsha.domain.Mass;
import by.imsha.domain.dto.MassDay;
import by.imsha.domain.dto.MassInfo;
import by.imsha.domain.dto.MassSchedule;
import by.imsha.meilisearch.model.Geo;
import by.imsha.meilisearch.model.Parish;
import by.imsha.meilisearch.model.SearchRecord;
import by.imsha.meilisearch.writer.MeilisearchWriter;
import by.imsha.utils.DateTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MassIndexService {

    private final DateTimeProvider dateTimeProvider;
    private final ScheduleFactory scheduleFactory;
    private final MassService massService;
    private final ParishService parishService;
    private final CityService cityService;
    private final MeilisearchWriter meilisearchWriter;

    public void updateIndex() {
        final List<SearchRecord> searchRecords = new LinkedList<>();
        final LocalDate today = dateTimeProvider.today();
        // вчера (-1 день)
        final LocalDate startDate = today.minusDays(1);
        // вчера + 6 дней недели
        final LocalDate endDate = startDate.plusDays(6);

        // ещё 2 дня, чтобы охватить текущую неделю +-1день
        final LocalDate additionalStartDate = endDate.plusDays(1);
        final LocalDate additionalEndDate = additionalStartDate.plusDays(1);

        for (City city : cityService.getAllCities()) {
            List<Mass> masses = massService.getMassByCity(city.getId());
            //исключаем парафии с состоянием "Ожидает подтверждения" и "Исходное состояние"
            final Set<String> pendingParishIds = parishService.getNotApprovedParishIds(city.getId());
            if (!pendingParishIds.isEmpty()) {
                masses = masses.stream()
                        .filter(mass -> !pendingParishIds.contains(mass.getParishId()))
                        .toList();
            }
            MassSchedule massHolder = scheduleFactory.build(masses, startDate, endDate);
            massHolder.createSchedule(7);
            List<MassDay> massDays = massHolder.getSchedule();

            appendMassDaysToSearchRecords(searchRecords, massDays, city);

            massHolder = scheduleFactory.build(masses, additionalStartDate, additionalEndDate);
            massHolder.createSchedule(2);
            massDays = massHolder.getSchedule();

            appendMassDaysToSearchRecords(searchRecords, massDays, city);
            //по итогу у нас 9 дней (неделя текущая и +-1 день)
        }

        meilisearchWriter.refreshAllData(searchRecords);
    }

    private void appendMassDaysToSearchRecords(List<SearchRecord> searchRecords, List<MassDay> schedule, City city) {

        for (MassDay massDay : schedule) {
            for (MassDay.MassHour massHour : massDay.getMassHours()) {
                for (MassInfo massInfo : massHour.getData()) {
                    Geo geo = Optional.ofNullable(massInfo.getParish().getGps())
                            .map(locationInfo ->
                                    Geo.builder()
                                            .lat((double) locationInfo.getLatitude())
                                            .lng((double) locationInfo.getLongitude())
                                            .build()
                            )
                            .orElse(null);
                    searchRecords.add(
                            SearchRecord.builder()
                                    .recordId(UUID.randomUUID().toString())
                                    .massId(massInfo.getId())
                                    .duration(3600)
                                    .dateTime(LocalDateTime.of(massDay.getDate(), massHour.getHour()))
                                    .parish(
                                            Parish.builder()
                                                    .id(massInfo.getParish().getParishId())
                                                    .actual(!massInfo.getParish().isNeedUpdate())
                                                    .state(parishService.getParish(massInfo.getParish().getParishId())
                                                            .map(by.imsha.domain.Parish::getState)
                                                            .map(Enum::name)
                                                            .orElseThrow())
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
                                    .lastModifiedDate(massInfo.getLastModifiedDate())
                                    .geo(geo)
                                    .build()
                    );
                }
            }
        }
    }
}
