package by.imsha.rest.passwordless.send;

import api_specification.by.imsha.server.fusionauth.public_client.api.FusionauthPublicApiClient;
import by.imsha.rest.passwordless.exception.PasswordlessApiException;
import by.imsha.rest.passwordless.mapper.FusionauthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@RequiredArgsConstructor
@Validated
@Slf4j
public class EmailCodeSender implements CodeSender {

    private final FusionauthPublicApiClient fusionauthPublicApiClient;
    private final FusionauthMapper fusionauthMapper;

    /**
     * Отправить код пользователю
     *
     * @param userIdentifier идентификатор пользователя (не используется, но может быть использован для реализации отправки через Telegram например)
     * @param code           отправляемый код
     */
    public void send(final String userIdentifier, final String code) {
        try {
            log.info("[VERBOSE] Send to: '{}', code: '{}'", userIdentifier, code);

            fusionauthPublicApiClient.sendCodeByEmail(
                    fusionauthMapper.map(code)
            );
        } catch (PasswordlessApiException passwordlessApiException) {
            throw passwordlessApiException;
        } catch (Exception exception) {
            throw new PasswordlessApiException("Ошибка при отправке кода",
                    false, exception);
        }
    }
}
