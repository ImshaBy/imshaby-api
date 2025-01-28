package by.imsha.validation.mass;

import by.imsha.utils.ServiceUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceUtilsTest {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    @CsvSource({
            "2025-04-15 11:11, 2025-04-15 11:11, 0, true",
            "2025-04-14 11:11, 2025-04-15 11:11, 0, true",
            "2025-04-15 11:11, 2025-04-15 11:11, 1, false",
            "2025-04-01 11:11, 2025-04-15 11:11, 14, true",
            "2025-04-01 11:11, 2025-04-15 11:12, 14, true",
            "2025-04-01 11:12, 2025-04-15 11:11, 14, false",
            "2025-04-12 00:00, 2025-04-15 11:11, 14, false",
    })
    @ParameterizedTest
    void testDateDiff(String dateAsString, String nowAsString, int updatePeriodInDays,
                      boolean needUpdate) {
        LocalDateTime date = LocalDateTime.parse(dateAsString, DATE_TIME_FORMATTER);
        LocalDateTime now = LocalDateTime.parse(nowAsString, DATE_TIME_FORMATTER);

        assertThat(ServiceUtils.needUpdateFromNow(date, now, updatePeriodInDays))
                .isEqualTo(needUpdate);
    }
}
