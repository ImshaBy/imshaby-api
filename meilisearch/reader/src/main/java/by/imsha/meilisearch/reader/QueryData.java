package by.imsha.meilisearch.reader;

import by.imsha.meilisearch.model.SearchRecord;
import lombok.Builder;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder
public record QueryData(
        String cityId,
        String parishId,
        String lang,
        Boolean online,
        Boolean rorate,
        LocalDate dateFrom,
        LocalDate dateTo
) {

    private static final String EQ_FILTER_TEMPLATE = "%s = %s";
    private static final String BETWEEN_FILTER_TEMPLATE = "%s %s TO %s";
    private static final String LE_FILTER_TEMPLATE = "%s <= %s";
    private static final String GE_FILTER_TEMPLATE = "%s >= %s";

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
        if (dateFrom != null && dateTo != null) {
            filters.add(new String[]{BETWEEN_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.DATE,
                    dateFrom.toEpochDay(), dateTo.toEpochDay())});
        } else if (dateFrom != null) {
            filters.add(new String[]{GE_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.DATE, dateFrom.toEpochDay())});
        } else if (dateTo != null) {
            filters.add(new String[]{LE_FILTER_TEMPLATE.formatted(SearchRecord.FilterableAttribute.DATE, dateTo.toEpochDay())});
        }

        return filters.isEmpty() ? null : filters.toArray(new String[1][filters.size()]);
    }
}
