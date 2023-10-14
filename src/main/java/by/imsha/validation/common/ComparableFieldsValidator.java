package by.imsha.validation.common;

import org.springframework.beans.BeanWrapperImpl;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Валидатор для проверки отношения сравниваемых значений
 * <p>
 * В случае провала проверки, ошибка привязывается к первому полю в списке
 */
public class ComparableFieldsValidator implements ConstraintValidator<ComparableFieldsValid, Object> {

    private static final Map<ComparableFieldsValid.Condition, Function<Integer, Boolean>> CONDITION_COMPARISON_RESULT_INTERPRETER_MAP;

    static {
        final Map<ComparableFieldsValid.Condition, Function<Integer, Boolean>> compareResultMap = new EnumMap<>(ComparableFieldsValid.Condition.class);

        compareResultMap.put(ComparableFieldsValid.Condition.EQUALS, cmpResult -> cmpResult == 0);
        compareResultMap.put(ComparableFieldsValid.Condition.NOT_EQUALS, cmpResult -> cmpResult != 0);
        compareResultMap.put(ComparableFieldsValid.Condition.LESS_THAN, cmpResult -> cmpResult < 0);
        compareResultMap.put(ComparableFieldsValid.Condition.GREATER_OR_EQUALS, cmpResult -> cmpResult >= 0);
        compareResultMap.put(ComparableFieldsValid.Condition.GREATER_THAN, cmpResult -> cmpResult > 0);
        compareResultMap.put(ComparableFieldsValid.Condition.LESS_OR_EQUALS, cmpResult -> cmpResult <= 0);

        CONDITION_COMPARISON_RESULT_INTERPRETER_MAP = Collections.unmodifiableMap(compareResultMap);
    }

    private ComparableFieldsValid constraintAnnotation;

    @Override
    public void initialize(ComparableFieldsValid constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext context) {
        final BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);

        final Object firstFieldValue = beanWrapper.getPropertyValue(constraintAnnotation.fields()[0]);
        //сравниваем только заполненные поля
        if (firstFieldValue == null) {
            return true;
        }

        final Object secondFieldValue = beanWrapper.getPropertyValue(constraintAnnotation.fields()[1]);
        //сравниваем только заполненные поля
        if (secondFieldValue == null) {
            return true;
        }

        //для текущего типа сравнения получаем интерпретатор результатов сравнения полей
        final boolean valid = CONDITION_COMPARISON_RESULT_INTERPRETER_MAP.get(constraintAnnotation.condition())
                .apply(
                        ((Comparable) firstFieldValue).compareTo(secondFieldValue)
                );

        if (!valid) {
            //если поля не прошли проверку, прикрепляем ошибку к первому из них
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(constraintAnnotation.fields()[0])
                    .addConstraintViolation();
            context.disableDefaultConstraintViolation();
        }

        return valid;
    }
}
