package by.imsha.rest.passwordless.send;

import lombok.Getter;

/**
 * Code interceptor, saves value instead of sending
 */
public class InterceptingCodeSender implements CodeSender {

    @Getter
    private String code;

    @Override
    public void send(String userIdentifier, String code) {
        this.code = code;
    }
}
