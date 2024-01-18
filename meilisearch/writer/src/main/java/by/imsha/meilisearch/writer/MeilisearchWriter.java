package by.imsha.meilisearch.writer;

import by.imsha.meilisearch.model.SearchRecord;

import java.util.List;

/**
 * Интерфейс, абстрагирующий операции модификации индекса
 */
public interface MeilisearchWriter {

    /**
     * Обновить данные по ключу парафии
     *
     * @param parishKey     уникальный ключ парафии
     * @param searchRecords актуальные данные
     */
    void refreshParishData(String parishKey, List<SearchRecord> searchRecords);

    /**
     * Заменить все данные актуальными
     *
     * @param searchRecords актуальные данные
     */
    void refreshAllData(List<SearchRecord> searchRecords);
}
