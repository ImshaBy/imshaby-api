package by.imsha.validation.mass;

import by.imsha.domain.Mass;
import by.imsha.service.MassService;
import by.imsha.utils.Constants;
import by.imsha.validation.ConstraintViolationPayloadBase64Coder;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static by.imsha.utils.Constants.WEEK_DAYS_COUNT;

/**
 * Валидатор уникальности мессы, проверяющий, что среди действующих (не удаленных) месс нет
 * пересечений по времени и дням недели
 * <p>
 * {@code null} считается валидным
 * В случае провала проверки, ошибка привязывается к полю
 */
@Slf4j
@RequiredArgsConstructor
public class UniqueMassValidator extends UniqueMassValidatorBase implements ConstraintValidator<UniqueMass, Mass> {

    //для закрепления ошибки за полем, используем поле time
    public static final String TIME_FIELD_NAME = "time";

    private final MassService massService;
    private final ConstraintViolationPayloadBase64Coder constraintViolationPayloadBase64Coder;

    @Override
    public boolean isValid(final Mass mass, final ConstraintValidatorContext context) {
        if (mass == null) {
            return true;
        }

        final Optional<ErrorPayload> error = checkMassUnique(mass);

        if (error.isPresent()) {
            addConstraintViolation(context, error.get());
            return false;
        }

        return true;
    }

    private Optional<ErrorPayload> checkMassUnique(final Mass currentMass) {
        //после перехода мессы в Periodic - идентификатор теряется, поэтому заранее сохраняем и используем дальше,
        //чтобы не возникло путаницы
        final String currentMassId = currentMass.getId();
        //для проверок нужна периодическая месса
        final Mass currentPeriodicMass = currentMass.asPeriodic();
        //для каждого дня текущей периодической мессы составляем битовую карту, в которой дни мессы отмечены true
        final boolean[] currentPeriodicMassDaysBitmap = new boolean[WEEK_DAYS_COUNT];
        Arrays.stream(currentPeriodicMass.getDays())
                .forEach(day -> currentPeriodicMassDaysBitmap[day - 1] = true);
        //вычисляем тип периода текущей периодической мессы
        final MassPeriodType currentPeriodicMassPeriodType = getMassPeriodType(currentPeriodicMass);
        //получаем мап, содержащий все калькуляторы общего периода, описанные для типа текущей мессы
        final Map<MassPeriodType, CommonPeriodCalculator> currentPeriodicMassCommonPeriodCalculatorMap = MASS_COMMON_PERIOD_CALCULATOR_MAP
                .get(currentPeriodicMassPeriodType);

        final List<Mass> allMasses = massService.getMassByParish(currentMass.getParishId());

        for (Mass anotherMass : allMasses) {
            //пропускаем текущую мессу (с таким же id) и удаленные
            if (anotherMass.getId().equals(currentMassId) || anotherMass.isDeleted()) {
                continue;
            }
            //трансформируем сравниваемую мессу в периодическую
            final Mass anotherPeriodicMass = anotherMass.asPeriodic();
            //пропускаем и те, время которых отличается от времени проверяемой мессы
            if (!anotherPeriodicMass.getTime().equals(currentPeriodicMass.getTime())) {
                continue;
            }
            //вычисляем тип периода сравниваемой периодической мессы
            final MassPeriodType anotherPeriodicMassType = getMassPeriodType(anotherPeriodicMass);
            //получаем необходимый для типов месс калькулятор общего периода
            final Period commonPeriod = currentPeriodicMassCommonPeriodCalculatorMap
                    .get(anotherPeriodicMassType)
                    .getCommonPeriod(currentPeriodicMass.getStartDate(), currentPeriodicMass.getEndDate(),
                            anotherPeriodicMass.getStartDate(), anotherPeriodicMass.getEndDate());
            //проверяем, есть ли пересечение между периодами действия
            // если общий период невалидный - пересечения нет, а значит это не дубликаты
            if (commonPeriod.isInvalid()) {
                continue;
            }
            //массив булевых значений, где для каждого дня сравниваемой мессы устанавливается соответствующее значение
            // из массива, заполненного для текущей мессы
            final boolean[] commonMassDaysBitmap = new boolean[WEEK_DAYS_COUNT];
            Arrays.stream(anotherPeriodicMass.getDays())
                    .forEach(day -> commonMassDaysBitmap[day - 1] = currentPeriodicMassDaysBitmap[day - 1]);

            if (hasCommonDays(commonPeriod, commonMassDaysBitmap)) {
                return Optional.of(
                        ErrorPayload.builder()
                                .duplicateMass(anotherMass)
                                .build()
                );
            }

        }
        return Optional.empty();
    }

    /**
     * Проверить, во время общего периода действия месс есть пересечения
     */
    private boolean hasCommonDays(final Period commonPeriod, final boolean[] commonMassDaysBitmap) {
        final LocalDate fromDate = commonPeriod.getStartDate();
        final LocalDate toDate = min(fromDate.plusWeeks(1), commonPeriod.getEndDate());

        for (LocalDate temp = fromDate; !temp.isAfter(toDate); temp = temp.plusDays(1)) {
            if (commonMassDaysBitmap[temp.getDayOfWeek().getValue() - 1]) {
                return true;
            }
        }

        return false;
    }

    /**
     * Добавить сообщение об ошибке, с вложеннным в него payload
     */
    private void addConstraintViolation(final ConstraintValidatorContext context, final ErrorPayload result) {
        try {
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate() + Constants.CONSTRAINT_VIOLATION_SEPARATOR
                            + constraintViolationPayloadBase64Coder.encode(result))
                    .addPropertyNode(TIME_FIELD_NAME)
                    .addConstraintViolation();
        } catch (JsonProcessingException e) {
            log.error("Unable to serialize error payload {}", result, e);

            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(TIME_FIELD_NAME)
                    .addConstraintViolation();
        }

        context.disableDefaultConstraintViolation();
    }

    /**
     * Результат проверки валидатором
     */
    @Builder
    @Value
    private static class ErrorPayload {

        Mass duplicateMass;
    }

}
