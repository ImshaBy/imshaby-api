package by.imsha.server.rest.passwordless.send;

import lombok.Getter;

/**
 * Перехватчик кода, сохраняет значение, вместо отправки
 */
public class InterceptingCodeSender implements CodeSender {

    @Getter
    private String code;

    @Override
    public void send(String userIdentifier, String code) {
        this.code = code;
    }
}
