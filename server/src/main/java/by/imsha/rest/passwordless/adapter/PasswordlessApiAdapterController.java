package by.imsha.rest.passwordless.adapter;

import by.imsha.rest.passwordless.handler.LoginHandler;
import by.imsha.rest.passwordless.handler.StartHandler;
import by.imsha.rest.passwordless.mapper.PasswordlessAdapterMapper;
import by.imsha.rest.passwordless.send.InterceptingCodeSender;
import api_specification.by.imsha.server.passwordless.adapter.server.api.PasswordlessAdapterApiController;
import api_specification.by.imsha.server.passwordless.adapter.server.model.FinishPasswordlessLoginRequest;
import api_specification.by.imsha.server.passwordless.adapter.server.model.FinishPasswordlessLoginResponse;
import api_specification.by.imsha.server.passwordless.adapter.server.model.GeneratePasswordlessLoginCodeRequest;
import api_specification.by.imsha.server.passwordless.adapter.server.model.GeneratePasswordlessLoginCodeResponse;
import api_specification.by.imsha.server.passwordless.adapter.server.model.StartPasswordlessLoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Контроллер-адаптер, для использования Passwordless API
 */
@Controller
@Slf4j
public class PasswordlessApiAdapterController extends PasswordlessAdapterApiController {

    private final StartHandler startHandler;
    private final LoginHandler loginHandler;
    private final PasswordlessAdapterMapper passwordlessAdapterMapper;

    public PasswordlessApiAdapterController(NativeWebRequest request, StartHandler startHandler, LoginHandler loginHandler, PasswordlessAdapterMapper passwordlessAdapterMapper) {
        super(request);
        this.startHandler = startHandler;
        this.loginHandler = loginHandler;
        this.passwordlessAdapterMapper = passwordlessAdapterMapper;
    }

    @Override
    public ResponseEntity<Void> startPasswordlessLogin(StartPasswordlessLoginRequest startPasswordlessLoginRequest) {

        startHandler.handle(
                passwordlessAdapterMapper.map(startPasswordlessLoginRequest)
        );

        return ResponseEntity.ok(null);
    }

    @Secured("ROLE_INTERNAL")
    @Override
    public ResponseEntity<GeneratePasswordlessLoginCodeResponse> generatePasswordlessLoginCode(GeneratePasswordlessLoginCodeRequest generatePasswordlessLoginCodeRequest) {

        //вместо отправки кода - перехватываем его
        final InterceptingCodeSender interceptingCodeSender = new InterceptingCodeSender();

        startHandler.handle(
                passwordlessAdapterMapper.map(generatePasswordlessLoginCodeRequest),
                interceptingCodeSender
        );

        //получаем перехваченный код
        final String code = interceptingCodeSender.getCode();

        return ResponseEntity.ok(
                passwordlessAdapterMapper.mapToGenerateResponse(code)
        );
    }

    @Override
    public ResponseEntity<FinishPasswordlessLoginResponse> finishPasswordlessLogin(FinishPasswordlessLoginRequest finishPasswordlessLoginRequest) {

        final String token = loginHandler.handle(
                passwordlessAdapterMapper.map(finishPasswordlessLoginRequest)
        );

        return ResponseEntity.ok(
                passwordlessAdapterMapper.mapToFinishResponse(token)
        );
    }

}
