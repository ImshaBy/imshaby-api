package by.imsha.rest.auth.exception;

/**
 * Исключение в процессе аутентификации
 */
public class AuthException extends RuntimeException {

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
