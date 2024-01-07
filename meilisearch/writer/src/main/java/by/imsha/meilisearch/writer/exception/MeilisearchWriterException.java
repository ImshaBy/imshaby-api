package by.imsha.meilisearch.writer.exception;

/**
 * Ошибка при записи в индекс
 */
public class MeilisearchWriterException extends RuntimeException {

    public MeilisearchWriterException() {
    }

    public MeilisearchWriterException(String message) {
        super(message);
    }

    public MeilisearchWriterException(String message, Throwable cause) {
        super(message, cause);
    }
}
