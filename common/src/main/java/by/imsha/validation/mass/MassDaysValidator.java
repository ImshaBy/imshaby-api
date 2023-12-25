package by.imsha.validation.mass;

import by.imsha.domain.Mass;
import by.imsha.utils.Constants;
import by.imsha.utils.DateTimeProvider;
import by.imsha.validation.ConstraintViolationPayloadBase64Coder;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static by.imsha.utils.Constants.WEEK_DAYS_COUNT;

/**
 * Валидатор дней мессы, проверяющий, что
 * <p>
 * 1) все значения в допустимом диапазоне<br>
 * 2) среди значений нет дубликатов<br>
 * 3) если период меньше недели, то указаны только те дни, которые попадают в период
 * <p>
 * Пустой массив считается валидным
 */
@Slf4j
@RequiredArgsConstructor
public class MassDaysValidator implements ConstraintValidator<MassDaysValid, Mass> {

    //для закрепления ошибки за полем, используем поле days
    public static final String DAYS_FIELD_NAME = "days";

    private final DateTimeProvider dateTimeProvider;
    private final ConstraintViolationPayloadBase64Coder constraintViolationPayloadBase64Coder;

    @Override
    public boolean isValid(final Mass mass, final ConstraintValidatorContext context) {
        //не проверяем пустые массивы
        if (ArrayUtils.isEmpty(mass.getDays())) {
            return true;
        }
        //проверяем, что содержатся допустимые значения
        if (!isNumbersValid(mass, context)) {
            return false;
        }
        //проверяем, что среди значений нет дубликатов
        if (!isNumberUnique(mass, context)) {
            return false;
        }

        //проверяем, что в случае, если период меньше недели, то указаны дни, входящие в этот период
        return isValid1WeekPeriod(mass, context);
    }

    /**
     * Добавить сообщение об ошибке, с вложеннным в него payload
     */
    private void addConstraintViolation(final ConstraintValidatorContext context, final Error result) {
        try {
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate() + Constants.CONSTRAINT_VIOLATION_SEPARATOR
                            + constraintViolationPayloadBase64Coder.encode(result))
                    .addPropertyNode(DAYS_FIELD_NAME)
                    .addConstraintViolation();
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize error payload {}", result, e);

            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(DAYS_FIELD_NAME)
                    .addConstraintViolation();
        }

        context.disableDefaultConstraintViolation();
    }

    /**
     * Проверить, что в случае, если период меньше недели, то указаны дни, входящие в этот период
     */
    private boolean isValid1WeekPeriod(final Mass mass, final ConstraintValidatorContext context) {
        final LocalDate endDate = mass.getEndDate();

        if (endDate == null) {
            return true;
        }

        final LocalDate startDate = mass.getStartDate() == null
                ? dateTimeProvider.today()
                : mass.getStartDate();

        //нет возможности проверить дни, если дата начала больше даты окончания
        if (startDate.isAfter(endDate)) {
            return true;
        }

        if (ChronoUnit.WEEKS.between(startDate, endDate) >= 1) {
            return true;
        }

        //набор дней мессы
        final Set<Integer> days = toSet(mass.getDays());

        //проходим по дням от startDate до endDate с шагом 1 день и удаляем из набора дней мессы совпадения
        for (LocalDate tempDate = startDate; !tempDate.isAfter(endDate); tempDate = tempDate.plusDays(1)) {
            days.remove(tempDate.getDayOfWeek().getValue());
        }

        if (!days.isEmpty()) {
            addConstraintViolation(
                    context,
                    Error.builder()
                            .type(Error.Type.NUMBER_NOT_AVAILABLE)
                            .days(days)
                            .build()
            );

            return false;
        }

        return true;
    }

    /**
     * Проверить, что среди значений нет дубликатов
     */
    private boolean isNumberUnique(final Mass mass, final ConstraintValidatorContext context) {
        final Set<Integer> distinctDays = toSet(mass.getDays());

        if (distinctDays.size() != mass.getDays().length) {
            final List<Integer> duplicatedDays = Arrays.stream(mass.getDays())
                    .filter(day -> !distinctDays.remove(day))
                    .boxed()
                    .collect(Collectors.toList());

            addConstraintViolation(
                    context,
                    Error.builder()
                            .type(Error.Type.DUPLICATED)
                            .days(duplicatedDays)
                            .build()
            );

            return false;
        }

        return true;
    }

    /**
     * Проверить, что содержатся допустимые значения
     */
    private boolean isNumbersValid(final Mass mass, final ConstraintValidatorContext context) {
        //значения в массиве должны быть в диапазоне от 1 до количества дней в неделе
        final List<Integer> wrongDayNumbers = new ArrayList<>();

        for (Integer day : mass.getDays()) {
            if (day == null || day < 1 || day > WEEK_DAYS_COUNT) {
                wrongDayNumbers.add(day);
            }
        }

        if (!wrongDayNumbers.isEmpty()) {
            addConstraintViolation(
                    context,
                    Error.builder()
                            .type(Error.Type.NUMBER_OUT_OF_RANGE)
                            .days(wrongDayNumbers)
                            .build()
            );
            return false;
        }

        return true;
    }

    /**
     * "Легковесная" реализация без стримов
     */
    private Set<Integer> toSet(final int[] days) {
        final HashSet<Integer> result = new HashSet<>();

        for (int day : days) {
            result.add(day);
        }

        return result;
    }

    /**
     * Результат проверки валидатором
     */
    @Builder
    @Value
    private static class Error {

        Type type;

        Collection<Integer> days;

        private enum Type {
            DUPLICATED,
            NUMBER_OUT_OF_RANGE,
            NUMBER_NOT_AVAILABLE,
        }
    }

}
