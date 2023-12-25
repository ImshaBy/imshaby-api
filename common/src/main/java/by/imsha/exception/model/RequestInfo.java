package by.imsha.exception.model;

import lombok.Builder;
import lombok.Value;

/**
 * Данные о HTTP-запросе
 */
@Value
@Builder
public class RequestInfo {

    /**
     * URI запроса
     */
    String uri;
    /**
     * HTTP метод запроса
     */
    String method;
    /**
     * TODO под вопросом немного
     */
    String pathInfo;
    /**
     * Параметры запроса
     */
    String query;
}
