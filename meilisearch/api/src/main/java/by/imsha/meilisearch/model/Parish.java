package by.imsha.meilisearch.model;

import lombok.Builder;

/**
 * Данные парафии (прихода)
 * <p>
 * В текущей реализации нет необходимости хранить что-то кроме id
 *
 * @param id идентификатор парафии
 */
@Builder
public record Parish(String id) {
}
