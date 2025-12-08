package by.imsha.rest.passwordless.send;

import api_specification.by.imsha.common.fusionauth.public_client.api.FusionauthPublicApiClient;
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
     * Send code to user
     *
     * @param userIdentifier user identifier (not used, but can be used for implementing sending via Telegram for example)
     * @param code           code to send
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
            throw new PasswordlessApiException("Error sending code",
                    false, exception);
        }
    }
}
