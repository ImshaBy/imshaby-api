package by.imsha.meilisearch.model;

import lombok.Builder;

/**
 * Данные города
 * <p>
 * В текущей реализации нет необходимости хранить что-то кроме id
 *
 * @param id идентификатор города
 */
@Builder
public record City(String id) {
}
