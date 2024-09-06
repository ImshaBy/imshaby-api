package by.imsha.rest.passwordless.adapter.response;

import lombok.Builder;
import lombok.Value;

/**
 * Тело ответа на запрос аутентификации
 */
@Value
@Builder
public class LoginResponse {

    /**
     * JWT токен
     */
    String token;
}
