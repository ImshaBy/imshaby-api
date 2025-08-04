package by.imsha.rest.auth.mapper;

import api_specification.by.imsha.server.fusionauth.secured_client.model.Address;
import api_specification.by.imsha.server.fusionauth.secured_client.model.SendConfirmationCode;
import api_specification.by.imsha.server.fusionauth.secured_client.model.SendEmailRequest;
import by.imsha.rest.auth.handler.RequestCodeHandler;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", implementationName = "FusionauthMapper_auth")
public interface FusionauthMapper {

    @Mapping(target = "requestData", source = "confirmationCode", qualifiedByName = "mapToSendConfirmationCode")
    @Mapping(target = "toAddresses", source = "input")
    SendEmailRequest map(String confirmationCode, RequestCodeHandler.Input input);

    @Named("mapToSendConfirmationCode")
    SendConfirmationCode mapToSendConfirmationCode(String confirmationCode);

    default List<Address> mapToAddresses(RequestCodeHandler.Input input) {

        return List.of(
                Address.builder()
                        .address(input.getEmail())
                        .build()
        );
    }
}
