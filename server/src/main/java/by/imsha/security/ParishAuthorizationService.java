package by.imsha.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Сервис для проверки прав доступа к приходам на основе parish keys в JWT токене
 */
@Service
@Slf4j
public class ParishAuthorizationService {

    private static final String PARISHES_CLAIM = "parishes";

    /**
     * Извлекает список ключей приходов из JWT токена текущего пользователя
     *
     * @return список ключей приходов, к которым у пользователя есть доступ
     */
    public List<String> getAuthorizedParishKeys() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.warn("Отсутствует аутентификация в SecurityContext");
            return Collections.emptyList();
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Jwt jwt)) {
            log.warn("Principal не является JWT токеном: {}", principal.getClass().getName());
            return Collections.emptyList();
        }

        Object parishesObject = jwt.getClaim(PARISHES_CLAIM);

        if (parishesObject == null) {
            log.info("JWT токен не содержит claim '{}' для пользователя: {}", PARISHES_CLAIM, jwt.getSubject());
            return Collections.emptyList();
        }

        if (parishesObject instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> parishes = (List<String>) parishesObject;
            log.debug("Пользователь {} имеет доступ к приходам: {}", jwt.getSubject(), parishes);
            return parishes;
        }

        log.warn("Claim '{}' имеет неожиданный тип: {}", PARISHES_CLAIM, parishesObject.getClass().getName());
        return Collections.emptyList();
    }

    /**
     * Проверяет, имеет ли текущий пользователь доступ к указанному приходу
     *
     * @param parishId идентификатор прихода
     * @throws AccessDeniedException если у пользователя нет доступа к приходу
     */
    public void checkParishAccess(String parishId) {
        if (parishId == null || parishId.isBlank()) {
            throw new IllegalArgumentException("Parish ID не должен быть пустым");
        }

        List<String> authorizedParishes = getAuthorizedParishKeys();

        if (authorizedParishes.isEmpty()) {
            log.error("Пользователь не имеет доступа ни к одному приходу");
            throw new AccessDeniedException("Отсутствуют права доступа к приходам");
        }

        if (!authorizedParishes.contains(parishId)) {
            log.error("Пользователь не имеет доступа к приходу: {}. Доступные приходы: {}", 
                    parishId, authorizedParishes);
            throw new AccessDeniedException("Отсутствует доступ к приходу: " + parishId);
        }

        log.debug("Доступ к приходу {} разрешен", parishId);
    }

    /**
     * Проверяет, имеет ли текущий пользователь доступ хотя бы к одному приходу
     *
     * @return true если у пользователя есть доступ хотя бы к одному приходу
     */
    public boolean hasAnyParishAccess() {
        return !getAuthorizedParishKeys().isEmpty();
    }
}

