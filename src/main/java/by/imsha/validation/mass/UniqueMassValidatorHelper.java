package by.imsha.validation.mass;

import by.imsha.domain.Mass;
import by.imsha.utils.ServiceUtils;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Базовый класс для проверки мессы на уникальность
 * <p>
 * здесь описаны все возможные варианты пересечений периодов и для каждого
 * варианта "простой" калькулятор периода (без всякой дополнительной логики)
 */
@Component
public class UniqueMassValidatorHelper {

    /**
     * Мап, в которой можно по двум типам периодов получить калькулятор для вычисления пересечений периодов
     */
    private static final Map<MassPeriodType, Map<MassPeriodType, CommonPeriodCalculator>> MASS_COMMON_PERIOD_CALCULATOR_MAP;
    /**
     * Значение, используемое как минимальная дата
     */
    public static final LocalDate MIN_DATE = ServiceUtils.timestampToLocalDate(0L).toLocalDate();

    static {
        //составляем мап пересечений всех со всеми (4 типа = 16 комбинаций)
        final Map<MassPeriodType, Map<MassPeriodType, CommonPeriodCalculator>> tempMap = new EnumMap<>(MassPeriodType.class);

        tempMap.computeIfAbsent(MassPeriodType.INFINITE, massPeriodType -> {
            final Map<MassPeriodType, CommonPeriodCalculator> map = new EnumMap<>(MassPeriodType.class);
            map.put(MassPeriodType.INFINITE, CommonPeriodCalculator.infiniteAndInfinite());
            map.put(MassPeriodType.BOUNDED_ABOVE, CommonPeriodCalculator.infiniteAndBoundedAbove());
            map.put(MassPeriodType.BOUNDED_BELOW, CommonPeriodCalculator.infiniteAndBoundedBelow());
            map.put(MassPeriodType.BOUNDED, CommonPeriodCalculator.infiniteAndBounded());
            return Collections.unmodifiableMap(map);
        });

        tempMap.computeIfAbsent(MassPeriodType.BOUNDED_ABOVE, massPeriodType -> {
            final Map<MassPeriodType, CommonPeriodCalculator> map = new EnumMap<>(MassPeriodType.class);
            map.put(MassPeriodType.INFINITE, CommonPeriodCalculator.boundedAboveAndInfinite());
            map.put(MassPeriodType.BOUNDED_ABOVE, CommonPeriodCalculator.boundedAboveAndBoundedAbove());
            map.put(MassPeriodType.BOUNDED_BELOW, CommonPeriodCalculator.boundedAboveAndBoundedBelow());
            map.put(MassPeriodType.BOUNDED, CommonPeriodCalculator.boundedAboveAndBounded());
            return Collections.unmodifiableMap(map);
        });

        tempMap.computeIfAbsent(MassPeriodType.BOUNDED_BELOW, massPeriodType -> {
            final Map<MassPeriodType, CommonPeriodCalculator> map = new EnumMap<>(MassPeriodType.class);
            map.put(MassPeriodType.INFINITE, CommonPeriodCalculator.boundedBelowAndInfinite());
            map.put(MassPeriodType.BOUNDED_ABOVE, CommonPeriodCalculator.boundedBelowAndBoundedAbove());
            map.put(MassPeriodType.BOUNDED_BELOW, CommonPeriodCalculator.boundedBelowAndBoundedBelow());
            map.put(MassPeriodType.BOUNDED, CommonPeriodCalculator.boundedBelowAndBounded());
            return Collections.unmodifiableMap(map);
        });

        tempMap.computeIfAbsent(MassPeriodType.BOUNDED, massPeriodType -> {
            final Map<MassPeriodType, CommonPeriodCalculator> map = new EnumMap<>(MassPeriodType.class);
            map.put(MassPeriodType.INFINITE, CommonPeriodCalculator.boundedAndInfinite());
            map.put(MassPeriodType.BOUNDED_ABOVE, CommonPeriodCalculator.boundedAndBoundedAbove());
            map.put(MassPeriodType.BOUNDED_BELOW, CommonPeriodCalculator.boundedAndBoundedBelow());
            map.put(MassPeriodType.BOUNDED, CommonPeriodCalculator.boundedAndBounded());
            return Collections.unmodifiableMap(map);
        });

        MASS_COMMON_PERIOD_CALCULATOR_MAP = Collections.unmodifiableMap(tempMap);
    }

    /**
     * Получить минимальную из двух дат
     */
    public static LocalDate min(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? second : first;
    }

    /**
     * Получить максимальную из двух дат
     */
    public static LocalDate max(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }

    /**
     * Получить тип периода мессы
     */
    public MassPeriodType getMassPeriodType(final Mass mass) {
        boolean boundedBelow = mass.getStartDate() != null;
        boolean boundedAbove = mass.getEndDate() != null;

        if (boundedBelow && boundedAbove) {
            return MassPeriodType.BOUNDED;
        } else if (!boundedBelow && !boundedAbove) {
            return MassPeriodType.INFINITE;
        } else {
            return boundedAbove ? MassPeriodType.BOUNDED_ABOVE : MassPeriodType.BOUNDED_BELOW;
        }
    }

    protected Map<MassPeriodType, CommonPeriodCalculator> getCalculatorMapForPeriodType(final MassPeriodType massPeriodType) {
        return MASS_COMMON_PERIOD_CALCULATOR_MAP.get(massPeriodType);
    }

    /**
     * Период (дата начала и конца)
     */
    @Value
    public static class Period {

        /**
         * Минимальный период относительно минимальной даты (1 неделя от минимальной даты)
         */
        public static final Period MIN_DATE_WEEK_PERIOD = new Period(MIN_DATE, MIN_DATE.plusWeeks(1));
        /**
         * Дата начала периода
         */
        LocalDate startDate;
        /**
         * Дата окончания периода
         */
        LocalDate endDate;
        /**
         * Определить, считается ли период невалидным
         * <p>
         * Период считается валидным, если startDate не больше endDate
         */
        boolean isInvalid() {
            return startDate.isAfter(endDate);
        }
    }

    /**
     * Тип периода действия мессы
     */
    public enum MassPeriodType {
        /**
         * Неограничен
         */
        INFINITE,
        /**
         * Ограничен снизу
         */
        BOUNDED_BELOW,
        /**
         * Ограничен сверху
         */
        BOUNDED_ABOVE,
        /**
         * Ограничена (сверху и снизу)
         */
        BOUNDED;
    }
    /**
     * Функциональный интерфейс для вычисления пересечения двух интервалов дат
     * <p>
     * вспомогательные методы для всех видов MassPeriodType (для упрощения написания нужных lambda)
     */
    public interface CommonPeriodCalculator {

        /**
         * Получить пересечение двух интервалов в виде периода дат
         */
        Period getCommonPeriod(LocalDate firstStartDate, LocalDate firstEndDate, LocalDate secondStartDate, LocalDate secondEndDate);

        static CommonPeriodCalculator infiniteAndInfinite() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> Period.MIN_DATE_WEEK_PERIOD;
        }

        static CommonPeriodCalculator infiniteAndBoundedAbove() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(MIN_DATE, secondEndDate);
        }

        static CommonPeriodCalculator boundedAboveAndInfinite() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(MIN_DATE, firstEndDate);
        }

        static CommonPeriodCalculator infiniteAndBoundedBelow() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(secondStartDate, secondStartDate.plusWeeks(1));
        }

        static CommonPeriodCalculator boundedBelowAndInfinite() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(firstStartDate, firstStartDate.plusWeeks(1));
        }

        static CommonPeriodCalculator infiniteAndBounded() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(secondStartDate, secondEndDate);
        }

        static CommonPeriodCalculator boundedAndInfinite() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(firstStartDate, firstEndDate);
        }

        static CommonPeriodCalculator boundedAboveAndBoundedAbove() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(MIN_DATE, min(firstEndDate, secondEndDate));
        }

        static CommonPeriodCalculator boundedAboveAndBoundedBelow() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(secondStartDate, firstEndDate);
        }

        static CommonPeriodCalculator boundedAboveAndBounded() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(secondStartDate, min(firstEndDate, secondEndDate));
        }

        static CommonPeriodCalculator boundedBelowAndBoundedAbove() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(firstStartDate, secondEndDate);
        }

        static CommonPeriodCalculator boundedBelowAndBoundedBelow() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> {
                final LocalDate startDate = max(firstStartDate, secondStartDate);
                return new Period(startDate, startDate.plusWeeks(1));
            };
        }

        static CommonPeriodCalculator boundedBelowAndBounded() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> {
                final LocalDate startDate = max(firstStartDate, secondStartDate);
                return new Period(startDate, secondEndDate);
            };
        }

        static CommonPeriodCalculator boundedAndBoundedAbove() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(firstStartDate, min(firstEndDate, secondEndDate));
        }


        static CommonPeriodCalculator boundedAndBoundedBelow() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> {
                final LocalDate startDate = max(firstStartDate, secondStartDate);
                return new Period(startDate, firstEndDate);
            };
        }

        static CommonPeriodCalculator boundedAndBounded() {
            return (firstStartDate, firstEndDate, secondStartDate, secondEndDate) -> new Period(max(firstStartDate, secondStartDate), min(firstEndDate, secondEndDate));
        }
    }
}
