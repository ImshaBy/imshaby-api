package by.imsha.server.exception.handler;

import by.imsha.exception.InvalidDateIntervalException;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.exception.model.ErrorResponse;
import by.imsha.exception.model.RequestInfo;
import by.imsha.server.rest.passwordless.exception.PasswordlessApiException;
import by.imsha.utils.DateTimeProvider;
import by.imsha.validation.ConstraintViolationPayloadBase64Coder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static by.imsha.utils.Constants.CONSTRAINT_VIOLATION_SEPARATOR;
import static java.util.Optional.ofNullable;

/**
 * Обработчик исключений, которые не были перехвачены в контроллере
 * <p>
 * в {@link ResponseEntityExceptionHandler} используется логгер, чтобы он не дублировались записи,
 * будем самостоятельно управлять логированием через primaryLogger
 */
@Slf4j(topic = "by.imsha.server.exception.handler.ApplicationExceptionHandler.primaryLogger")
@ControllerAdvice
@RequiredArgsConstructor
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Исключение, для замены исключений, логировать которые нет смысла
     */
    private static final RuntimeException NOT_LOGGABLE_EXCEPTION = new RuntimeException("NOT LOGGABLE EXCEPTION");

    private final DateTimeProvider dateTimeProvider;
    private final ConstraintViolationPayloadBase64Coder constraintViolationPayloadBase64Coder;
    private final MessageSource messageSource;

    /**
     * Обрабатываем все ошибки, которые не были обработаны остальными обработчиками
     *
     * @param exception  обрабатываемое исключение
     * @param webRequest http запрос
     * @return {@link ResponseEntity} , соответствующий ошибке
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleUncaughtException(final Exception exception, final WebRequest webRequest) {

        return handleExceptionInternal(exception, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, webRequest);
    }

    /**
     * Обрабатываем ошибки валидации
     *
     * @param exception  обрабатываемое исключение
     * @param webRequest http запрос
     * @return {@link ResponseEntity} , соответствующий ошибке
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolationException(final ConstraintViolationException exception,
                                                                        final WebRequest webRequest) {

        final List<ErrorResponse.FieldError> errors = new ArrayList<>();

        final Set<ConstraintViolation<?>> constraintViolations = exception.getConstraintViolations();
        if (!CollectionUtils.isEmpty(constraintViolations)) {
            constraintViolations.forEach(constraintViolation -> errors.add(convertToResponseError(constraintViolation)));
        }

        return handleExceptionInternal(exception, new FieldErrorsWrapper(errors), new HttpHeaders(),
                HttpStatus.BAD_REQUEST, webRequest);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException exception,
                                                                  final HttpHeaders headers,
                                                                  final HttpStatusCode statusCode,
                                                                  final WebRequest request) {
        final List<ErrorResponse.FieldError> errors = new ArrayList<>();

        final List<FieldError> fieldErrors = exception.getFieldErrors();
        if (!CollectionUtils.isEmpty(fieldErrors)) {
            fieldErrors.forEach(fieldError -> errors.add(convertToResponseError(fieldError)));
        }

        if (!CollectionUtils.isEmpty(exception.getGlobalErrors())) {
            //в текущем подходе используем только ошибки привязанные к полям,
            // глобальные ошибки нужно заменить на ошибки полей
            log.error("[FIXME] Global error are in use!!!");
        }
        return handleExceptionInternal(exception, new FieldErrorsWrapper(errors), headers, statusCode, request);
    }

    /**
     * Обрабатываем ResourceNotFoundException
     * <p>
     * единый формат, payload пустой, статус 404
     *
     * @param exception  обрабатываемое исключение
     * @param webRequest http запрос
     * @return {@link ResponseEntity} , соответствующий ошибке
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleResourceNotFoundException(final ResourceNotFoundException exception, final WebRequest webRequest) {

        return handleExceptionInternal(exception, null, new HttpHeaders(), HttpStatus.NOT_FOUND, webRequest);
    }

    /**
     * Обрабатываем InvalidDateIntervalException
     * <p>
     * единый формат, payload пустой, статус 400
     *
     * @param exception  обрабатываемое исключение
     * @param webRequest http запрос
     * @return {@link ResponseEntity} , соответствующий ошибке
     */
    @ExceptionHandler(InvalidDateIntervalException.class)
    protected ResponseEntity<Object> handleInvalidDateIntervalException(final InvalidDateIntervalException exception, final WebRequest webRequest) {

        final String code = exception.getCode();
        final String field = exception.getField();

        return handleExceptionInternal(
                exception,
                new FieldErrorsWrapper(
                        Collections.singletonList(
                                ErrorResponse.FieldError.builder()
                                        .code(code)
                                        .field(field)
                                        .message(getMessage(field, code))
                                        .build()
                        )
                ),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                webRequest
        );
    }

    @ExceptionHandler(PasswordlessApiException.class)
    public ResponseEntity<Object> handleException(final PasswordlessApiException exception, final WebRequest webRequest) {
        if (exception.isNotifiable()) {
            return handleExceptionInternal(
                    exception,
                    null,
                    new HttpHeaders(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    webRequest
            );
        } else {
            log.error("Passwordless API request handling exception! ", exception);

            return ResponseEntity.ok().build();
        }

    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(final @NonNull Exception exception,
                                                             final @Nullable Object body,
                                                             final @NonNull HttpHeaders headers,
                                                             final @NonNull HttpStatusCode statusCode,
                                                             final @NonNull WebRequest request) {


        final ResponseEntity<Object> responseEntity = super.handleExceptionInternal(
                exception,
                ofNullable(body)
                        .map(data -> {
                            //все методы приведены к использованию ErrorDetailsPartWrapper
                            // или null (базовые методы родительского класса)
                            if (data instanceof FieldErrorsWrapper wrapper) {
                                return ErrorResponse.builder()
                                        .timestamp(dateTimeProvider.nowZoned())
                                        .requestInfo(getRequestInfo(request))
                                        .errors(wrapper.errors)
                                        .build();

                            }
                            return data;
                        })
                        .orElse(null),
                headers,
                statusCode,
                request
        );

        //чтобы не логировать исключение, нужно заменить его на константу
        if (exception != NOT_LOGGABLE_EXCEPTION) {
            log.error("API request handling exception! Error response body: {}", responseEntity, exception);
        }

        return responseEntity;
    }

    @Override
    protected @NonNull ResponseEntity<Object> createResponseEntity(@Nullable Object body, @NonNull HttpHeaders headers, @NonNull HttpStatusCode statusCode, @NonNull WebRequest request) {

        if (!(body instanceof ErrorResponse)) {
            //все ошибки без оболочки игнорируем
            body = ErrorResponse.builder()
                    .timestamp(dateTimeProvider.nowZoned())
                    .requestInfo(getRequestInfo(request))
                    .build();
        }

        return super.createResponseEntity(body, headers, statusCode, request);
    }

    /**
     * Получить по коду ошибки ее описание
     *
     * @param code код ошибки
     * @return сообщение, соответствующее ключу {@code "constraint." + code}
     */
    protected String getMessage(final String field, final String code) {
        final String key = field + "." + code;
        try {
            return messageSource.getMessage(key, null, Locale.getDefault());
        } catch (Exception exception) {
            log.error("No message for key - '{}'", key, exception);
            return null;
        }
    }

    /**
     * Получить данные о запросе
     *
     * @param webRequest выполняемый запрос
     * @return данные о запросе
     */
    protected RequestInfo getRequestInfo(WebRequest webRequest) {
        HttpServletRequest request = ((ServletWebRequest) webRequest).getRequest();
        return RequestInfo.builder()
                .method(request.getMethod())
                .uri(request.getRequestURI())
                .query(request.getQueryString())
                .pathInfo(request.getPathInfo())
                .build();
    }

    private ErrorResponse.FieldError convertToResponseError(FieldError fieldError) {
        final String message = fieldError.getDefaultMessage();
        final String field = fieldError.getField();

        if (message == null) {
            log.error("Constraint violation has no message - '{}'", fieldError);

            return ErrorResponse.FieldError.builder()
                    .field(field)
                    .build();
        }

        //сообщения об ошибке могут содержать код и полезную нагрузку в формате json
        final String[] messageParts = message.split(CONSTRAINT_VIOLATION_SEPARATOR);

        Object payload = null;
        try {
            payload = messageParts.length == 2 ? constraintViolationPayloadBase64Coder.decode(messageParts[1]) : null;
        } catch (Exception e) {
            log.error("Failed to convert payload from json - '{}'", messageParts[1]);
        }

        final String code = messageParts[0];

        return ErrorResponse.FieldError.builder()
                .field(field)
                .code(code)
                .payload(payload)
                .message(getMessage(field, code))
                .build();
    }


    private ErrorResponse.FieldError convertToResponseError(ConstraintViolation<?> constraintViolation) {
        final String fieldName = determineField(constraintViolation);
        final String message = constraintViolation.getMessage();
        if (message == null) {
            log.error("Constraint violation has no message - '{}'", constraintViolation);

            return ErrorResponse.FieldError.builder()
                    .field(fieldName)
                    .build();
        }

        //сообщения об ошибке могут содержать код и полезную нагрузку в формате json
        final String[] messageParts = message.split(CONSTRAINT_VIOLATION_SEPARATOR);

        Object payload = null;
        try {
            payload = messageParts.length == 2 ? constraintViolationPayloadBase64Coder.decode(messageParts[1]) : null;
        } catch (Exception e) {
            log.error("Failed to convert payload from json - '{}'", messageParts[1]);
        }

        final String code = messageParts[0];

        return ErrorResponse.FieldError.builder()
                .field(fieldName)
                .code(code)
                .payload(payload)
                .message(getMessage(fieldName, code))
                .build();
    }


    /**
     * Одолжил у спринга, получения наименования поля
     *
     * @see org.springframework.validation.beanvalidation.SpringValidatorAdapter#determineField(ConstraintViolation)
     */
    protected String determineField(ConstraintViolation<?> violation) {
        Path path = violation.getPropertyPath();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Path.Node node : path) {
            if (node.isInIterable()) {
                sb.append('[');
                Object index = node.getIndex();
                if (index == null) {
                    index = node.getKey();
                }
                if (index != null) {
                    sb.append(index);
                }
                sb.append(']');
            }
            String name = node.getName();
            if (name != null && node.getKind() == ElementKind.PROPERTY && !name.startsWith("<")) {
                if (!first) {
                    sb.append('.');
                }
                first = false;
                sb.append(name);
            }
        }
        return sb.toString();
    }

    /**
     * Вспомогательный класс обёртка
     */
    @RequiredArgsConstructor
    private static class FieldErrorsWrapper {

        private final List<ErrorResponse.FieldError> errors;
    }
}
