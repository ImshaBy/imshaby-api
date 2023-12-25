package by.imsha.validation.mass;

import by.imsha.domain.Mass;
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
public class MassGroupSequenceProvider implements DefaultGroupSequenceProvider<Mass> {

    /**
     * Получить группы валидаций для проверки мессы
     *
     * @param mass проверяемая месса
     * @return группы валидаций
     */
    public List<Class<?>> getValidationGroups(final Mass mass) {

        final List<Class<?>> defaultGroupSequence = new ArrayList<>();
        //вместо Default группы должен быть сам класс
        defaultGroupSequence.add(Mass.class);
        if (mass == null) {
            return defaultGroupSequence;
        }
        //добавляем группу, на основе периодичности месс
        defaultGroupSequence.add(
                mass.isPeriodic()
                        ? MassGroups.Periodic.class
                        : MassGroups.Single.class
        );
        //последней добавляем группу по проверке дубликатов (должна быть последней, так как тяжеловесная)
        defaultGroupSequence.add(MassGroups.Duplicate.class);

        return defaultGroupSequence;
    }
}