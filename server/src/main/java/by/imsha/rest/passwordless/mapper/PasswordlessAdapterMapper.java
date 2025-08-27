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
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class PasswordlessAdapterMapper {

    @Autowired
    protected FusionauthProperties passwordlessApiProperties;

    @Mapping(source = "email", target = "loginId")
    @Mapping(target = "applicationId", expression = "java( passwordlessApiProperties.getApplicationId() )")
    public abstract StartHandler.Input map(StartPasswordlessLoginRequest request);

    @Mapping(source = "email", target = "loginId")
    @Mapping(target = "applicationId", expression = "java( passwordlessApiProperties.getApplicationId() )")
    public abstract StartHandler.Input map(GeneratePasswordlessLoginCodeRequest request);

    public abstract LoginHandler.Input map(FinishPasswordlessLoginRequest finishPasswordlessLoginRequest);

    public abstract GeneratePasswordlessLoginCodeResponse mapToGenerateResponse(String code);

    public abstract FinishPasswordlessLoginResponse mapToFinishResponse(String token);
}
