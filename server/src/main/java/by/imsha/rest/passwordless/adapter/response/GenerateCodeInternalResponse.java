package by.imsha.rest.passwordless.adapter.response;

import lombok.Builder;
import lombok.Value;

/**
 * Тело ответа на запрос, для получения кода для беспарольной аутентификации
 */
@Value
@Builder
public class GenerateCodeInternalResponse {

    /**
     * Код для беспарольной аутентификации
     */
    String code;
}
