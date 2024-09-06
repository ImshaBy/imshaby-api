package by.imsha.server.bdd.glue.steps.request;

import by.imsha.server.ImshabyApiEndpoint;
import by.imsha.server.bdd.glue.components.request.body.factory.RequestBodyModifier;
import io.cucumber.spring.ScenarioScope;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
@Setter
@Component
@ScenarioScope
public class HttpRequestData {

    public static final String API_KEY_HEADER_NAME = "X-Api-Key";
    public static final String TEST_API_KEY = "TEST_API_KEY";
    public static final String INTERNAL_TEST_API_KEY = "TEST_INTERNAL_API_KEY";

    private ImshabyApiEndpoint endpoint;
    private Map<String, String> pathVariables;
    private Map<String, String> requestParams;
    private Map<String, String> requestHeaders;
    private String requestBody;
    /**
     * Не очищается в cleanUp, т.к. заполняется до подготовки запроса
     */
    private Function<RequestSpecification, RequestSpecification> authorizationFunction;
    /**
     * Модификаторы тела запроса
     */
    private List<RequestBodyModifier> requestBodyModifiers;

    public HttpRequestData addRequestBodyModifier(final RequestBodyModifier requestBodyModifier) {
        getRequestBodyModifiers().add(requestBodyModifier);
        return this;
    }

    public void cleanUp() {
        setEndpoint(null);
        setPathVariables(null);
        setRequestParams(null);
        setRequestHeaders(null);
        setRequestBody(null);
        setRequestBodyModifiers(new ArrayList<>());
    }
}
