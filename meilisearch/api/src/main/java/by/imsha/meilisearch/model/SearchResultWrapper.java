package by.imsha.meilisearch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Представление для результата поиска
 *
 * @param hits               найденные совпадения
 * @param facetDistribution  распределение фасетов
 * @param processingTimeMs   время обработки запроса
 * @param query              использованная строка запроса
 * @param offset             смещение при поиске
 * @param limit              максимальное значение при поиске
 * @param estimatedTotalHits предполагаемое полное количество совпадений
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResultWrapper(
        List<SearchResultItem> hits,
        Map<String, Map<String, Integer>> facetDistribution,
        int processingTimeMs,
        String query,
        int offset,
        int limit,
        int estimatedTotalHits) {
}
