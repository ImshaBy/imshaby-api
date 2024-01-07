package by.imsha.meilisearch.reader.exception;

/**
 * Ошибка при чтении из индекса
 */
public class MeilisearchReaderException extends RuntimeException {

    public MeilisearchReaderException() {
    }

    public MeilisearchReaderException(String message) {
        super(message);
    }

    public MeilisearchReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
