package by.imsha.rest.auth.controller.response;

import lombok.Builder;
import lombok.Value;

/**
 * Тело ответа на запрос проверки кода подтверждения
 */
@Value
@Builder
public class VerifyConfirmationCodeResponse {

    /**
     * Признак валидности кода подтверждения
     */
    Boolean valid;
}

