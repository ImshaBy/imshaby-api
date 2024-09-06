package by.imsha.server.bdd.glue.steps;

import by.imsha.server.ImshabyApiEndpoint;
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.ValidatableResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * Контейнер ответа для возможности разделения методов валидации ответа по отдельным классам
 */
@Getter
@Setter
@ScenarioScope
@Component
public class HttpResponseHolder {

    private ValidatableResponse validatableResponse;
    private ImshabyApiEndpoint imshabyApiEndpoint;

    public void cleanUp() {
        setValidatableResponse(null);
        setImshabyApiEndpoint(null);
    }
}

