package by.imsha.rest.passwordless.exception;

import lombok.Getter;

/**
 * Исключение, при работе с passwordless api
 */
public class PasswordlessApiException extends RuntimeException {

    /**
     * Флаг, указывающий, может ли ошибка сообщаться пользователю
     */
    @Getter
    private final boolean notifiable;

    public PasswordlessApiException(String message, boolean notifiable) {
        super(message);
        this.notifiable = notifiable;
    }

    public PasswordlessApiException(String message, boolean notifiable, Throwable cause) {
        super(message, cause);
        this.notifiable = notifiable;
    }
}
