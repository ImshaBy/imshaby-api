package by.imsha.validation.mass;

/**
 * Группы валидаций для месс
 */
public class MassGroups {

    /**
     * Относящиеся к периодическим мессам
     */
    public interface Periodic {
    }

    /**
     * Относящиеся к одинарным мессам
     */
    public interface Single {
    }

    /**
     * Проверки дублирования (тяжёлая проверка, так как потребуется обратиться к хранилищу)
     */
    public interface Duplicate {
    }

}
