package by.imsha.meilisearch.reader;

import by.imsha.meilisearch.model.SearchRecord;
import lombok.Builder;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Builder
public record MassSearchFilter(
        String cityId,
        String parishId,
        String lang,
        Boolean online,
        Boolean rorate,
        OffsetDateTime dateTimeFrom,
        OffsetDateTime dateTimeTo
) {

    private static final String EQ_FILTER_TEMPLATE = "%s = %s";
    private static final String BETWEEN_FILTER_TEMPLATE = "%s %s TO %s";
    private static final String LE_FILTER_TEMPLATE = "%s <= %s";
    private static final String GE_FILTER_TEMPLATE = "%s >= %s";

    //TODO всюду используется зона +3 (нужно хорошо всё обдумать и отрефакторить)
    private static final ZoneOffset BEL_ZONE_OFFSET = ZoneOffset.ofHours(3);

    public String[][] toFilterArray() {
        final List<String[]> filters = new ArrayList<>();

        if (StringUtils.hasText(cityId)) {
            filters.add(new String[]{EQ_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.CITY_ID, cityId)});
        }
        if (StringUtils.hasText(parishId)) {
            filters.add(new String[]{EQ_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.PARISH_ID, parishId)});
        }
        if (StringUtils.hasText(lang)) {
            filters.add(new String[]{EQ_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.LANG, lang)});
        }
        if (online != null) {
            filters.add(new String[]{EQ_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.ONLINE, online)});
        }
        if (rorate != null) {
            filters.add(new String[]{EQ_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.RORATE, rorate)});
        }
        if (dateTimeFrom != null && dateTimeTo != null) {
            filters.add(new String[]{BETWEEN_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.DATE_TIME,
                    dateTimeFrom.atZoneSameInstant(BEL_ZONE_OFFSET).toEpochSecond(),
                    dateTimeTo.atZoneSameInstant(BEL_ZONE_OFFSET).toEpochSecond())});
        } else if (dateTimeFrom != null) {
            filters.add(new String[]{GE_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.DATE_TIME,
                    dateTimeFrom.atZoneSameInstant(BEL_ZONE_OFFSET).toEpochSecond())});
        } else if (dateTimeTo != null) {
            filters.add(new String[]{LE_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.DATE_TIME,
                    dateTimeTo.atZoneSameInstant(BEL_ZONE_OFFSET).toEpochSecond())});
        }

        return filters.isEmpty() ? null : filters.toArray(new String[1][filters.size()]);
    }
}
