package by.imsha.validation.mass;

import by.imsha.domain.Mass;
import by.imsha.service.MassService;
import by.imsha.utils.Constants;
import by.imsha.validation.ConstraintViolationPayloadBase64Coder;
import tools.jackson.core.JacksonException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;


/**
 * Валидатор уникальности мессы, проверяющий, что среди действующих (не удаленных) месс нет
 * пересечений по времени и дням недели
 * <p>
 * {@code null} считается валидным
 * В случае провала проверки, ошибка привязывается к полю
 */
@Slf4j
@RequiredArgsConstructor
public class UniqueMassValidator implements ConstraintValidator<UniqueMass, Mass> {

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

    private Optional<ErrorPayload> checkMassUnique(Mass mass) {
        return massService.findScheduleConflict(mass)
                .map(duplicate -> ErrorPayload.builder().duplicateMass(duplicate).build());
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
        } catch (JacksonException e) {
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
