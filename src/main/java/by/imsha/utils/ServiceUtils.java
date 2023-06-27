package by.imsha.utils;

import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static by.imsha.utils.Constants.LIMIT;
import static by.imsha.utils.Constants.PAGE;

/**
 * @author Alena Misan
 */
public class ServiceUtils {

    private static String dateFormat = "dd-MM-yyyy";
    private static String langParamName = "lang";

    private static String timeFormat = "dd-MM-yyyy HH:mm";

    public static final ZoneId BEL_ZONE_ID = ZoneId.of("Europe/Minsk");

    private static final Logger log = LoggerFactory.getLogger(ServiceUtils.class);


    public static String[] parseSortValue(String sort){
        if(StringUtils.isBlank(sort)){
            return null;
        }

        String[] values = new String[2];
        if(sort.length() > 1 && (sort.startsWith("+") || sort.startsWith("-"))){
            values[0]=sort.substring(1);
            values[1]=String.valueOf(sort.charAt(0));
        }else{
            values[0] = sort;
            values[1] = "+";
        }
        return values;

    }


    public static LocalDateTime timestampToLocalDate(long timestamp, ZoneId zoneId){
        return ZonedDateTime.ofInstant ( Instant.ofEpochSecond ( timestamp ) , zoneId ).toLocalDateTime();
    }

    public static LocalDateTime timestampToLocalDate(long timestamp){
        return timestampToLocalDate(timestamp, BEL_ZONE_ID);
    }

    public static long dateToUTCTimestamp(String day) throws DateTimeParseException{
        LocalDate date = null;
        if(day != null){
            try{
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
                date = LocalDate.parse(day, formatter);
            }catch (DateTimeParseException ex){
                throw new DateTimeParseException(String.format("Date format is incorrect. Date - %s,format - %s ", day, dateFormat), ex.getParsedString(), ex.getErrorIndex());
            }
        }

        return date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
    }

    public static ZonedDateTime localDateTimeToZoneDateTime(LocalDateTime localDateTime, ZoneId fromZone, ZoneId toZone) {
        ZonedDateTime date = ZonedDateTime.of(localDateTime, fromZone);
        return date.withZoneSameInstant(toZone);
    }

    public static int[] calculateOffsetAndLimit(int offset, int limit){
        int page = PAGE;
        int limitPerPage = LIMIT;
        if(offset > -1 && limit > 0){
            if(offset % limit == 0){
                page = offset;
                limitPerPage = limit;
            }else{
                page = PAGE;
                limitPerPage = limit;
            }
        }else if( limit < 0 && offset > 0 && offset % LIMIT == 0){
            page = offset;
            limitPerPage = LIMIT;
        } else if(offset < 0 && limit > 0 && PAGE % limit == 0 ){
            page = PAGE;
            limitPerPage = limit;
        }
        int[] result = new int[2];
        result[0] = page;
        result[1] = limitPerPage;
        return result;

    }
    public static Optional<HttpServletRequest> getCurrentHttpRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(requestAttributes -> ServletRequestAttributes.class.isAssignableFrom(requestAttributes.getClass()))
                .map(requestAttributes -> ((ServletRequestAttributes) requestAttributes))
                .map(ServletRequestAttributes::getRequest);
    }

    public static String fetchUserLangFromHttpRequest(){
        Optional<HttpServletRequest> httpServletRequest = ServiceUtils.getCurrentHttpRequest();
        if(httpServletRequest.isPresent()){
            String paramLang = httpServletRequest.get().getParameter(langParamName);
            if(StringUtils.isEmpty(paramLang)){
                //it's ok to determine lang as part of locale as fallback user language
                paramLang = RequestContextUtils.getLocale(httpServletRequest.get()).getLanguage();
            }
            return paramLang;
        }
        return Constants.DEFAULT_LANG;
    }
    public static Query buildMongoQuery(String sort, int page, int limitPerPage, Condition<GeneralQueryBuilder> condition, MongoVisitor mongoVisitor) {
        Criteria criteria = condition.query(mongoVisitor);
        Query query = new Query();
        query.addCriteria(criteria);
        query.with(PageRequest.of(page, limitPerPage));
        String[] sortValue = ServiceUtils.parseSortValue(sort);
        if(sortValue != null){
            Sort.Direction direction = sortValue[1].equals("+") ? Sort.Direction.ASC : Sort.Direction.DESC;
            query.with(Sort.by(direction, sortValue[0]));
        }
        return query;
    }
    public static long dateTimeToUTCTimestamp(String day) throws DateTimeParseException{
        LocalDateTime date = null;
        if(day != null){
            try{
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeFormat);
                date = LocalDateTime.parse(day, formatter);
            }catch (DateTimeParseException ex){
                throw new DateTimeParseException(String.format("Date time format is incorrect. Date time - %s,format - %s ", day, timeFormat), ex.getParsedString(), ex.getErrorIndex());
            }
        }

        return date.toEpochSecond(ZoneOffset.UTC);
    }

    /**
     *
     * */
    public static boolean needUpdateFromNow(LocalDateTime pLastModifiedDate, int pUpdatePeriodInDays) {
        LocalDateTime now = LocalDateTime.now();
        boolean result;
        ZonedDateTime nowTime = ServiceUtils.localDateTimeToZoneDateTime(now, ZoneId.systemDefault(), BEL_ZONE_ID);
        if(pLastModifiedDate == null){
            result = true;
        }else{
            result = Math.abs(ChronoUnit.DAYS.between(nowTime.toLocalDate(), pLastModifiedDate.toLocalDate().minusDays(1))) > pUpdatePeriodInDays;
        }
        return result;
    }

    public static long hourDiff(LocalDateTime localDateTimeFrom, LocalDateTime localDateTimeTo){
        return Math.abs(ChronoUnit.HOURS.between(localDateTimeFrom, localDateTimeTo));
    }


}
