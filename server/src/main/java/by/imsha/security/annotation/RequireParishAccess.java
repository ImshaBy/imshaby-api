package by.imsha.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для проверки прав доступа к приходу на основе JWT токена.
 * Используется с AOP для автоматической валидации прав доступа.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireParishAccess {
    
    /**
     * Имя path variable, содержащего идентификатор прихода
     */
    String parishIdParam() default "parishId";
    
    /**
     * Извлекать идентификатор прихода из тела запроса вместо path variable
     */
    boolean fromRequestBody() default false;
    
    /**
     * Имя поля в теле запроса, содержащего идентификатор прихода
     */
    String bodyField() default "parishId";
    
    /**
     * Имя path variable, содержащего идентификатор мессы.
     * Если указан, будет выполнен поиск мессы для получения parishId
     */
    String massIdParam() default "";
}
