package by.imsha.rest.passwordless.adapter;

import by.imsha.rest.passwordless.PasswordlessApiProperties;
import by.imsha.rest.passwordless.adapter.request.LoginRequest;
import by.imsha.rest.passwordless.adapter.request.StartRequest;
import by.imsha.rest.passwordless.adapter.response.LoginResponse;
import by.imsha.rest.passwordless.handler.LoginHandler;
import by.imsha.rest.passwordless.handler.StartHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/**
 * Контроллер-адаптер, для использования Passwordless API
 */
@RestController
@RequestMapping("/api/passwordless")
@RequiredArgsConstructor
public class PasswordlessApiAdapter {

    private final PasswordlessApiProperties passwordlessApiProperties;
    private final StartHandler startHandler;
    private final LoginHandler loginHandler;

    @PostMapping("/start")
    public void start(@RequestBody @Valid StartRequest request) {
        startHandler.handle(
                StartHandler.Input.builder()
                        .loginId(request.getEmail())
                        .applicationId(passwordlessApiProperties.getApplicationId())
                        .build()
        );
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        final String token = loginHandler.handle(
                LoginHandler.Input.builder()
                        .code(request.getCode())
                        .build()
        );
        return LoginResponse.builder()
                .token(token)
                .build();
    }

    //TODO убрать, просто пока для демонстрации работы
    @PostMapping("/claims")
    public Map<String, Object> token(@AuthenticationPrincipal Jwt principal) {
        return principal.getClaims();
    }
}
