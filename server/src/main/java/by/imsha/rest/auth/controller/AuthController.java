package by.imsha.rest.auth.controller;

import by.imsha.rest.auth.controller.request.SendConfirmationCodeRequest;
import by.imsha.rest.auth.controller.request.VerifyConfirmationCodeRequest;
import by.imsha.rest.auth.controller.response.VerifyConfirmationCodeResponse;
import by.imsha.rest.auth.handler.RequestCodeHandler;
import by.imsha.rest.auth.handler.VerifyCodeHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для процесса аутентификации
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final RequestCodeHandler requestCodeHandler;
    private final VerifyCodeHandler verifyCodeHandler;

    @Secured("ROLE_INTERNAL")
    @PostMapping("/request-code")
    public void requestCode(@RequestBody @Valid SendConfirmationCodeRequest request) {

        requestCodeHandler.handle(
                RequestCodeHandler.Input.builder()
                        .email(request.getEmail())
                        .build()
        );
    }

    @Secured("ROLE_INTERNAL")
    @PostMapping("/verify-code")
    public VerifyConfirmationCodeResponse verifyCode(@RequestBody @Valid VerifyConfirmationCodeRequest request) {

        boolean valid = verifyCodeHandler.handle(
                VerifyCodeHandler.Input.builder()
                        .email(request.getEmail())
                        .confirmationCode(request.getConfirmationCode())
                        .build()
        );

        return VerifyConfirmationCodeResponse.builder()
                .valid(valid)
                .build();
    }
}
