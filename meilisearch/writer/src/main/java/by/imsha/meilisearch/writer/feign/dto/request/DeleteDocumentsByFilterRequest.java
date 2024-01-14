package by.imsha.meilisearch.writer.feign.dto.request;

import lombok.Builder;

/**
 * Данные для запроса на удаление документов по фильтру
 *
 * @param filter строка с фильтрующим выражением
 */
@Builder
public record DeleteDocumentsByFilterRequest(String filter) {
}
