package by.imsha.rest.auth.controller;

import api_specification.by.imsha.server.auth.server.api.AuthApiController;
import api_specification.by.imsha.server.auth.server.model.CodeVerificationResponse;
import api_specification.by.imsha.server.auth.server.model.SendVerificationCodeToEmailRequest;
import api_specification.by.imsha.server.auth.server.model.VerifyCodeRequest;
import by.imsha.rest.auth.handler.RequestCodeHandler;
import by.imsha.rest.auth.handler.VerifyCodeHandler;
import by.imsha.rest.auth.mapper.AuthApiMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Контроллер для процесса аутентификации
 */
@Controller
public class AuthController extends AuthApiController {

    private final RequestCodeHandler requestCodeHandler;
    private final VerifyCodeHandler verifyCodeHandler;
    private final AuthApiMapper authApiMapper;

    public AuthController(NativeWebRequest request, RequestCodeHandler requestCodeHandler, VerifyCodeHandler verifyCodeHandler, AuthApiMapper authApiMapper) {
        super(request);
        this.requestCodeHandler = requestCodeHandler;
        this.verifyCodeHandler = verifyCodeHandler;
        this.authApiMapper = authApiMapper;
    }

    @Secured("ROLE_INTERNAL")
    @Override
    public ResponseEntity<Void> sendVerificationCodeToEmail(SendVerificationCodeToEmailRequest sendVerificationCodeToEmailRequest) {

        requestCodeHandler.handle(
                authApiMapper.map(sendVerificationCodeToEmailRequest)
        );

        return ResponseEntity.ok(null);
    }

    @Secured("ROLE_INTERNAL")
    @Override
    public ResponseEntity<CodeVerificationResponse> verifyCode(VerifyCodeRequest verifyCodeRequest) {

        boolean valid = verifyCodeHandler.handle(
                authApiMapper.map(verifyCodeRequest)
        );

        return ResponseEntity.ok(
                authApiMapper.mapToVerificationResponse(valid)
        );
    }
}
