package by.imsha.rest.auth.mapper;

import api_specification.by.imsha.server.auth.server.model.CodeVerificationResponse;
import api_specification.by.imsha.server.auth.server.model.SendVerificationCodeToEmailRequest;
import api_specification.by.imsha.server.auth.server.model.VerifyCodeRequest;
import by.imsha.rest.auth.handler.RequestCodeHandler;
import by.imsha.rest.auth.handler.VerifyCodeHandler;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthApiMapper {

    RequestCodeHandler.Input map(SendVerificationCodeToEmailRequest sendVerificationCodeToEmailRequest);

    VerifyCodeHandler.Input map(VerifyCodeRequest verifyCodeRequest);

    CodeVerificationResponse mapToVerificationResponse(Boolean valid);
}
