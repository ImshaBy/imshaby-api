package by.imsha.exception;

import lombok.Getter;

/**
 * Исключение, в случае невалидного порядка дат, с полями идентифицирующими ошибку
 */
public class InvalidDateIntervalException extends RuntimeException {

    @Getter
    private final String field;
    @Getter
    private final String code;

    public InvalidDateIntervalException(final String message, final String field, final String code) {
        super(message);
        this.field = field;
        this.code = code;
    }

}
