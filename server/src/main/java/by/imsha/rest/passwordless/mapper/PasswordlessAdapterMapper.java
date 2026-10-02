package by.imsha.rest.passwordless.mapper;

import api_specification.by.imsha.server.passwordless.adapter.server.model.FinishPasswordlessLoginRequest;
import api_specification.by.imsha.server.passwordless.adapter.server.model.FinishPasswordlessLoginResponse;
import api_specification.by.imsha.server.passwordless.adapter.server.model.GeneratePasswordlessLoginCodeRequest;
import api_specification.by.imsha.server.passwordless.adapter.server.model.GeneratePasswordlessLoginCodeResponse;
import api_specification.by.imsha.server.passwordless.adapter.server.model.StartPasswordlessLoginRequest;
import by.imsha.properties.FusionauthProperties;
import by.imsha.rest.passwordless.handler.LoginHandler;
import by.imsha.rest.passwordless.handler.StartHandler;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Mapper(componentModel = "spring")
public abstract class PasswordlessAdapterMapper {

    @Autowired
    protected FusionauthProperties passwordlessApiProperties;

    @Mapping(source = "email", target = "loginId")
    @Mapping(target = "applicationId", expression = "java( resolveApplicationId( request.getApplicationId() ) )")
    public abstract StartHandler.Input map(StartPasswordlessLoginRequest request);

    @Mapping(source = "email", target = "loginId")
    @Mapping(target = "applicationId", expression = "java( passwordlessApiProperties.getApplicationId() )")
    public abstract StartHandler.Input map(GeneratePasswordlessLoginCodeRequest request);

    /**
     * Приложение, в которое запрашивается вход: из запроса, если оно разрешено, иначе по умолчанию.
     *
     * @param requested идентификатор из запроса, может отсутствовать
     * @return идентификатор приложения для FusionAuth
     * @throws ResponseStatusException 400, если приложение не входит в список разрешённых
     */
    // @Named обязателен: без него MapStruct счёл бы метод общим преобразованием String -> String
    // и применил бы ко всем строковым полям (email, код из письма), а не только к applicationId.
    @Named("resolveApplicationId")
    protected String resolveApplicationId(final String requested) {
        final String defaultApplicationId = passwordlessApiProperties.getApplicationId();

        if (requested == null || requested.isBlank() || requested.trim().equals(defaultApplicationId)) {
            return defaultApplicationId;
        }

        final String applicationId = requested.trim();
        if (passwordlessApiProperties.getAllowedApplicationIds().contains(applicationId)) {
            return applicationId;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Приложение не поддерживается");
    }

    public abstract LoginHandler.Input map(FinishPasswordlessLoginRequest finishPasswordlessLoginRequest);

    public abstract GeneratePasswordlessLoginCodeResponse mapToGenerateResponse(String code);

    public abstract FinishPasswordlessLoginResponse mapToFinishResponse(String token);
}
