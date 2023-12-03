package by.imsha.utils;

import java.time.temporal.ChronoUnit;

/**
 * @author Alena Misan
 */
public class Constants {

    private Constants(){
        //to avoid initializations
    }

    public static int LIMIT = 10;

    public static int PAGE = 0;

    public static String DEFAULT_LANG = "be";

    public static String ONLINE_FILTER = "online";
    public static String RORATE_FILTER = "rorate";
    public static final String  DEFAULT_PAGE_SIZE = "40";
    public static final String DEFAULT_PAGE_NUM = "0";

    public static final Integer  DEFAULT_PAGE_SIZE_INT = Integer.parseInt(DEFAULT_PAGE_SIZE);
    public static final Integer DEFAULT_PAGE_NUM_INT =  Integer.parseInt(DEFAULT_PAGE_NUM);



    public static String DATE_FORMAT = "MM/dd/yyyy";

    /**
     * Для реализации возможности определять ошибки с полезной нагрузкой
     */
    public static final String CONSTRAINT_VIOLATION_SEPARATOR = "__CV_MARKER__";

    /**
     * Количество дней в неделе
     */
    public static final int WEEK_DAYS_COUNT = (int) ChronoUnit.WEEKS.getDuration().toDays();

}
