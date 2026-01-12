package by.imsha.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParishAuthorizationServiceTest {

    private final ParishAuthorizationService service = new ParishAuthorizationService();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnEmptyListWhenNoAuthentication() {
        // Given
        SecurityContextHolder.clearContext();

        // When
        List<String> result = service.getAuthorizedParishKeys();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenPrincipalIsNotJwt() {
        // Given
        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("not-a-jwt");
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        // When
        List<String> result = service.getAuthorizedParishKeys();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenJwtHasNoParishesClaim() {
        // Given
        Jwt jwt = createJwt(Collections.emptyMap());
        setupJwtAuthentication(jwt);

        // When
        List<String> result = service.getAuthorizedParishKeys();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnParishKeysFromJwtClaim() {
        // Given
        List<String> parishes = List.of("parish1", "parish2", "parish3");
        Jwt jwt = createJwt(Map.of("parishes", parishes));
        setupJwtAuthentication(jwt);

        // When
        List<String> result = service.getAuthorizedParishKeys();

        // Then
        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder("parish1", "parish2", "parish3");
    }

    @Test
    void shouldReturnEmptyListWhenParishesClaimIsNotList() {
        // Given
        Jwt jwt = createJwt(Map.of("parishes", "not-a-list"));
        setupJwtAuthentication(jwt);

        // When
        List<String> result = service.getAuthorizedParishKeys();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void checkParishKeyAccess_shouldAllowAccessWhenKeyIsAuthorized() {
        // Given
        List<String> parishes = List.of("parish1", "parish2");
        Jwt jwt = createJwt(Map.of("parishes", parishes));
        setupJwtAuthentication(jwt);

        // When/Then
        assertThat(service.getAuthorizedParishKeys()).contains("parish1");
        // No exception should be thrown
        service.checkParishKeyAccess("parish1");
    }

    @Test
    void checkParishKeyAccess_shouldDenyAccessWhenKeyIsNotAuthorized() {
        // Given
        List<String> parishes = List.of("parish1", "parish2");
        Jwt jwt = createJwt(Map.of("parishes", parishes));
        setupJwtAuthentication(jwt);

        // When/Then
        assertThatThrownBy(() -> service.checkParishKeyAccess("parish3"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Отсутствует доступ к приходу: parish3");
    }

    @Test
    void checkParishKeyAccess_shouldThrowExceptionWhenNoParishes() {
        // Given
        Jwt jwt = createJwt(Collections.emptyMap());
        setupJwtAuthentication(jwt);

        // When/Then
        assertThatThrownBy(() -> service.checkParishKeyAccess("parish1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Отсутствуют права доступа к приходам");
    }

    @Test
    void checkParishKeyAccess_shouldThrowExceptionForNullKey() {
        // Given
        List<String> parishes = List.of("parish1");
        Jwt jwt = createJwt(Map.of("parishes", parishes));
        setupJwtAuthentication(jwt);

        // When/Then
        assertThatThrownBy(() -> service.checkParishKeyAccess(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parish key не должен быть пустым");
    }

    @Test
    void checkParishKeyAccess_shouldThrowExceptionForBlankKey() {
        // Given
        List<String> parishes = List.of("parish1");
        Jwt jwt = createJwt(Map.of("parishes", parishes));
        setupJwtAuthentication(jwt);

        // When/Then
        assertThatThrownBy(() -> service.checkParishKeyAccess("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parish key не должен быть пустым");
    }

    @Test
    void isInternalRole_shouldReturnTrueWhenUserHasInternalRole() {
        // Given
        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        java.util.Collection<? extends GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_INTERNAL")
        );
        when(authentication.getAuthorities()).thenReturn((java.util.Collection<GrantedAuthority>) authorities);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        // When
        boolean result = service.isInternalRole();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isInternalRole_shouldReturnFalseWhenUserDoesNotHaveInternalRole() {
        // Given
        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        java.util.Collection<? extends GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );
        when(authentication.getAuthorities()).thenReturn((java.util.Collection<GrantedAuthority>) authorities);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        // When
        boolean result = service.isInternalRole();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isInternalRole_shouldReturnFalseWhenNoAuthentication() {
        // Given
        SecurityContextHolder.clearContext();

        // When
        boolean result = service.isInternalRole();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasAnyParishAccess_shouldReturnTrueWhenUserHasParishes() {
        // Given
        List<String> parishes = List.of("parish1");
        Jwt jwt = createJwt(Map.of("parishes", parishes));
        setupJwtAuthentication(jwt);

        // When
        boolean result = service.hasAnyParishAccess();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasAnyParishAccess_shouldReturnFalseWhenUserHasNoParishes() {
        // Given
        Jwt jwt = createJwt(Collections.emptyMap());
        setupJwtAuthentication(jwt);

        // When
        boolean result = service.hasAnyParishAccess();

        // Then
        assertThat(result).isFalse();
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                claims
        );
    }

    private void setupJwtAuthentication(Jwt jwt) {
        SecurityContext context = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }
}
