package by.imsha.meilisearch.model;

import lombok.Builder;

/**
 * Данные парафии (прихода)
 * <p>
 * В текущей реализации нет необходимости хранить что-то кроме id
 *
 * @param id идентификатор парафии
 * @param actual признак актуальности расписания парафии
 * @param state состояние парафии (подтверждена, создана и т.п.)
 */
@Builder
public record Parish(String id, boolean actual, String state) {
}
