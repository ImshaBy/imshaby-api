package by.imsha.rest.passwordless.mapper;

import api_specification.by.imsha.server.fusionauth.public_client.model.SendCodeByEmailRequest;
import api_specification.by.imsha.server.fusionauth.secured_client.model.StartPasswordlessLoginRequest;
import by.imsha.rest.passwordless.handler.LoginHandler;
import by.imsha.rest.passwordless.handler.StartHandler;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", implementationName = "FusionauthMapper_passwordless")
public interface FusionauthMapper {

    SendCodeByEmailRequest map(LoginHandler.Input input);

    StartPasswordlessLoginRequest map(StartHandler.Input input);

    SendCodeByEmailRequest map(String code);
}
