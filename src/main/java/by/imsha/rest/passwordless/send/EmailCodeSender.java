package by.imsha.rest.passwordless.send;

import by.imsha.properties.PasswordlessApiProperties;
import by.imsha.rest.passwordless.exception.PasswordlessApiException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Validated
@Slf4j
public class EmailCodeSender implements CodeSender {

    private final PasswordlessApiProperties passwordlessApiProperties;
    private final RestTemplate passwordlessPublicRestTemplate;

    /**
     * Отправить код пользователю
     *
     * @param userIdentifier идентификатор пользователя (не используется, но может быть использован для реализации отправки через Telegram например)
     * @param code           отправляемый код
     */
    public void send(final String userIdentifier, final String code) {
        try {
            log.info("[VERBOSE] Send to: '{}', code: '{}'", userIdentifier, code);

            ResponseEntity<Void> exchange = passwordlessPublicRestTemplate.exchange(
                    RequestEntity.post(passwordlessApiProperties.getUri().getSend())
                            .body(RequestBody.builder()
                                    .code(code)
                                    .build()),
                    Void.class
            );
            if (!exchange.getStatusCode().is2xxSuccessful()) {
                throw new PasswordlessApiException("Получен ответ с HTTP-кодом " + exchange.getStatusCodeValue(), false);
            }
        } catch (PasswordlessApiException passwordlessApiException) {
            throw passwordlessApiException;
        } catch (Exception exception) {
            throw new PasswordlessApiException("Ошибка при отправке кода",
                    false, exception);
        }
    }

    @Builder
    @Value
    private static class RequestBody {
        /**
         * Уникальный код, необходимый для завершения входа в систему
         */
        String code;
    }
}
