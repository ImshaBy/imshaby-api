package by.imsha.rest.passwordless.exception;

import lombok.Getter;

/**
 * Exception when working with passwordless api
 */
public class PasswordlessApiException extends RuntimeException {

    /**
     * Flag indicating whether the error can be communicated to the user
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
