package by.imsha.rest.passwordless.send;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleCodeSender implements CodeSender {

    @Override
    public void send(String userIdentifier, String code) {
        log.info("Сгенерированный для {} код = '{}'", userIdentifier, code);
    }
}
