package by.imsha.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class DateTimeProvider {

    private final Clock clock;

    /**
     * Получить текущую дату
     *
     * @return текущая дата
     */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Получить текущее время и дату
     */
    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * Получить текущее время и дату в зоне по умолчанию
     */
    public LocalDateTime nowSystemDefaultZone() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }

    /**
     * Получить текущее время с информацией о зоне
     *
     * @return текущее время
     */
    public ZonedDateTime nowZoned() {
        return ZonedDateTime.now(clock);
    }
}
