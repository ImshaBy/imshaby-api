package by.imsha.service;

import by.imsha.domain.City;
import by.imsha.domain.Mass;
import by.imsha.meilisearch.model.Geo;
import by.imsha.meilisearch.model.Parish;
import by.imsha.meilisearch.model.SearchRecord;
import by.imsha.meilisearch.writer.MeilisearchWriter;
import by.imsha.utils.DateTimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static by.imsha.utils.LocalizedUtils.getLocalizedMassNotes;
import static by.imsha.utils.ServiceUtils.BEL_ZONE_ID;
import static by.imsha.utils.UserLocaleHolder.getUserLocale;

import java.time.Instant;
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
    private final MassOccurrenceFactory occurrenceFactory;
    private final MassService massService;
    private final ParishService parishService;
    private final CityService cityService;
    private final MeilisearchWriter meilisearchWriter;

    public void updateIndex() {
        final List<SearchRecord> searchRecords = new LinkedList<>();
        LocalDate startDate = dateTimeProvider.today().minusDays(1);
        Instant from = startDate.atStartOfDay(BEL_ZONE_ID).toInstant();
        Instant toExclusive = startDate.plusDays(9).atStartOfDay(BEL_ZONE_ID).toInstant();

        for (City city : cityService.getAllCities()) {
            List<Mass> masses = massService.getMassByCity(city.getId());
            //исключаем парафии с состоянием "Ожидает подтверждения" и "Исходное состояние"
            final Set<String> pendingParishIds = parishService.getNotApprovedParishIds(city.getId());
            if (!pendingParishIds.isEmpty()) {
                masses = masses.stream()
                        .filter(mass -> !pendingParishIds.contains(mass.getParishId()))
                        .toList();
            }
            for (Mass mass : masses) {
                if (mass.getParishId() == null) {
                    continue;
                }
                parishService.getParish(mass.getParishId()).ifPresent(parish ->
                        occurrenceFactory.occurrences(mass, from, toExclusive)
                                .map(occurrence -> toSearchRecord(occurrence, city, parish))
                                .forEach(searchRecords::add));
            }
        }

        meilisearchWriter.refreshAllData(searchRecords);
    }

    private SearchRecord toSearchRecord(MassOccurrenceFactory.Occurrence occurrence, City city,
                                        by.imsha.domain.Parish parish) {
        Mass mass = occurrence.mass();
        Geo geo = Optional.ofNullable(parish.getGps())
                .map(location -> Geo.builder().lat((double) location.getLatitude())
                        .lng((double) location.getLongitude()).build())
                .orElse(null);
        return SearchRecord.builder()
                .recordId(UUID.randomUUID().toString())
                .massId(mass.getId())
                .duration(Optional.ofNullable(mass.getDuration()).orElse(3600L).intValue())
                .dateTime(LocalDateTime.ofInstant(occurrence.start(), BEL_ZONE_ID))
                .parish(Parish.builder().id(parish.getId()).actual(!parish.isNeedUpdate())
                        .state(parish.getState().name()).build())
                .notes(getLocalizedMassNotes(mass, getUserLocale().orElse(null)))
                .lang(mass.getLangCode())
                .online(Boolean.TRUE.equals(mass.getOnline()))
                .rorate(Boolean.TRUE.equals(mass.getRorate()))
                .city(by.imsha.meilisearch.model.City.builder().id(city.getId()).build())
                .lastModifiedDate(mass.getLastModifiedDate())
                .geo(geo)
                .build();
    }
}
