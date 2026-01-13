package by.imsha.security.aspect;

import by.imsha.domain.Mass;
import by.imsha.domain.Parish;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.security.ParishAuthorizationService;
import by.imsha.security.annotation.RequireParishAccess;
import by.imsha.service.MassService;
import by.imsha.service.ParishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * AOP аспект для автоматической проверки прав доступа к приходам.
 * Перехватывает методы с аннотацией @RequireParishAccess.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ParishAccessAspect {

    private final ParishAuthorizationService parishAuthorizationService;
    private final ParishService parishService;
    private final MassService massService;

    @Before("@annotation(requireParishAccess)")
    public void checkParishAccess(JoinPoint joinPoint, RequireParishAccess requireParishAccess) {
        log.debug("Проверка прав доступа к приходу для метода: {}", joinPoint.getSignature().getName());

        // Пропускаем проверку для пользователей с ROLE_INTERNAL
        if (parishAuthorizationService.isInternalRole()) {
            log.debug("Пользователь имеет ROLE_INTERNAL, проверка прав доступа пропущена");
            return;
        }

        String parishId = extractParishId(joinPoint, requireParishAccess);
        
        if (parishId == null || parishId.isBlank()) {
            log.warn("Не удалось извлечь идентификатор прихода для метода: {}", joinPoint.getSignature().getName());
            throw new IllegalArgumentException("Parish ID не найден в параметрах запроса");
        }

        // Получаем приход по ID для извлечения ключа
        Parish parish = parishService.getParish(parishId)
                .orElseThrow(() -> new ResourceNotFoundException("Приход не найден: " + parishId));

        // Проверяем доступ по ключу прихода
        parishAuthorizationService.checkParishKeyAccess(parish.getKey());
        
        log.debug("Проверка прав доступа успешна для прихода: {} (key: {})", parishId, parish.getKey());
    }

    /**
     * Извлекает идентификатор прихода из параметров метода
     */
    private String extractParishId(JoinPoint joinPoint, RequireParishAccess annotation) {
        // Если указан massId, сначала получаем массу
        if (!annotation.massIdParam().isEmpty()) {
            String massId = extractPathVariable(joinPoint, annotation.massIdParam());
            if (massId != null) {
                Mass mass = massService.getMass(massId)
                        .orElseThrow(() -> new ResourceNotFoundException("Месса не найдена: " + massId));
                return mass.getParishId();
            }
        }

        // Если нужно извлечь из тела запроса
        if (annotation.fromRequestBody()) {
            return extractFromRequestBody(joinPoint, annotation.bodyField());
        }

        // По умолчанию извлекаем из path variable
        return extractPathVariable(joinPoint, annotation.parishIdParam());
    }

    /**
     * Извлекает значение path variable по имени
     */
    private String extractPathVariable(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String name = pathVariable.value().isEmpty() ? pathVariable.name() : pathVariable.value();
                if (name.isEmpty()) {
                    name = parameters[i].getName();
                }
                if (name.equals(paramName) && args[i] != null) {
                    return args[i].toString();
                }
            }
        }
        return null;
    }

    /**
     * Извлекает значение поля из объекта в теле запроса
     */
    private String extractFromRequestBody(JoinPoint joinPoint, String fieldName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            RequestBody requestBody = parameters[i].getAnnotation(RequestBody.class);
            if (requestBody != null && args[i] != null) {
                return extractFieldValue(args[i], fieldName);
            }
        }
        return null;
    }

    /**
     * Извлекает значение поля из объекта
     */
    private String extractFieldValue(Object obj, String fieldName) {
        try {
            return Optional.of(new BeanWrapperImpl(obj))
                    .map(beanWrapper -> beanWrapper.getPropertyValue(fieldName))
                    .map(Object::toString)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Ошибка при извлечении поля '{}' из объекта {}", fieldName, obj.getClass().getName(), e);
        }
        return null;
    }
}
