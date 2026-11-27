package by.imsha.validation.mass;

import by.imsha.domain.DeprecatedMass;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Поставщик групп валидации на основании состояния объекта мессы
 * <p>
 * Все группы проходят последовательную проверку, и в каком порядке группы будут получены, в том и будут проверены.
 * <p>
 * Важно!!! Если при валидации группы была обнаружена ошибка, валидация следующей группы не происходит.
 */
public class DeprecatedMassGroupSequenceProvider implements DefaultGroupSequenceProvider<DeprecatedMass> {

    /**
     * Получить группы валидаций для проверки мессы
     *
     * @param mass проверяемая месса
     * @return группы валидаций
     */
    public List<Class<?>> getValidationGroups(final DeprecatedMass mass) {

        final List<Class<?>> defaultGroupSequence = new ArrayList<>();
        //вместо Default группы должен быть сам класс
        defaultGroupSequence.add(DeprecatedMass.class);
        if (mass == null) {
            return defaultGroupSequence;
        }
        //добавляем группу, на основе периодичности месс
        defaultGroupSequence.add(
                mass.isPeriodic()
                        ? MassGroups.Periodic.class
                        : MassGroups.Single.class
        );
        if (mass.isPeriodic()) {
            defaultGroupSequence.add(MassGroups.StartWeekday.class);
        }

        return defaultGroupSequence;
    }
}
