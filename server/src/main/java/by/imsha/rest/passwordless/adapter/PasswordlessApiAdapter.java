package by.imsha.rest.passwordless.adapter;

import by.imsha.properties.PasswordlessApiProperties;
import by.imsha.rest.passwordless.adapter.request.GenerateCodeInternalRequest;
import by.imsha.rest.passwordless.adapter.request.LoginRequest;
import by.imsha.rest.passwordless.adapter.request.StartRequest;
import by.imsha.rest.passwordless.adapter.response.GenerateCodeInternalResponse;
import by.imsha.rest.passwordless.adapter.response.LoginResponse;
import by.imsha.rest.passwordless.handler.StartHandler;
import by.imsha.rest.passwordless.send.InterceptingCodeSender;
import by.imsha.rest.passwordless.handler.LoginHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер-адаптер, для использования Passwordless API
 */
@RestController
@RequestMapping("/api/passwordless")
@RequiredArgsConstructor
@Slf4j
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

    @Secured("ROLE_INTERNAL")
    @PostMapping(value = "/code")
    public GenerateCodeInternalResponse internalGenerateCode(@RequestBody @Valid GenerateCodeInternalRequest request) {
        //вместо отправки кода - перехватываем его
        final InterceptingCodeSender interceptingCodeSender = new InterceptingCodeSender();

        startHandler.handle(
                StartHandler.Input.builder()
                        .loginId(request.getEmail())
                        .applicationId(passwordlessApiProperties.getApplicationId())
                        .build(),
                interceptingCodeSender
        );

        //получаем перехваченный код
        final String code = interceptingCodeSender.getCode();

        return GenerateCodeInternalResponse.builder()
                .code(code)
                .build();
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

}
