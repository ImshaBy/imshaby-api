package by.imsha.exception;

public class InvalidFilterException extends RuntimeException {
    public InvalidFilterException(String filter, Throwable cause) {
        super("Invalid filter: " + filter, cause);
    }
}
