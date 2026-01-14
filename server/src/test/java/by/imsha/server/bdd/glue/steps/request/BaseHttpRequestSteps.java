package by.imsha.server.bdd.glue.steps.request;

import by.imsha.server.ImshabyApiEndpoint;
import by.imsha.server.bdd.glue.components.GlobalStorage;
import by.imsha.server.bdd.glue.components.request.body.factory.RequestBodyModifier;
import by.imsha.server.bdd.glue.steps.HttpResponseHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cucumber.datatable.DataTable;
import io.cucumber.docstring.DocString;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Collectors;

import static by.imsha.server.bdd.glue.steps.request.HttpRequestData.API_KEY_HEADER_NAME;
import static by.imsha.server.bdd.glue.steps.request.HttpRequestData.AUTHORIZATION_HEADER_NAME;
import static by.imsha.server.bdd.glue.steps.request.HttpRequestData.INTERNAL_TEST_API_KEY;
import static by.imsha.server.bdd.glue.steps.request.HttpRequestData.TEST_API_KEY;
import static io.restassured.RestAssured.given;

public class BaseHttpRequestSteps {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private HttpRequestData httpRequestData;
    @Autowired
    private HttpResponseHolder httpResponseHolder;
    @Autowired
    private GlobalStorage globalStorage;

    /**
     * Использовать при отправке запросов авторизацию через api-токен
     * пример использования
     * <p>
     * ИСПОЛЬЗУЕТСЯ В БЛОКЕ Предыстория ДЛЯ ИСПОЛЬЗОВАНИЯ В НЕСКОЛЬКИХ СЦЕНАРИЯХ В ОДНОМ ФАЙЛЕ
     * <p>
     * Предыстория:
     * __Допустим при запросе не используются никакие способы авторизации
     */
    @And("^при запросе не используются никакие способы авторизации$")
    public void cleanAuthorizationFunction() {
        httpRequestData.setAuthorizationFunction(null);
    }

    /**
     * Использовать при отправке запросов авторизацию через jwt
     * пример использования
     * <p>
     * ИСПОЛЬЗУЕТСЯ В БЛОКЕ Предыстория ДЛЯ ИСПОЛЬЗОВАНИЯ В НЕСКОЛЬКИХ СЦЕНАРИЯХ В ОДНОМ ФАЙЛЕ
     * <p>
     * Предыстория:
     * __Допустим для авторизации используется jwt-токен с ключом парафии "parishKey"
     */
    @And("^для авторизации используется jwt-токен с ключом парафии \"(.+)\"$")
    public void authorizeWithJWTAndParishKey(String parishKey) {
        httpRequestData.setAuthorizationFunction(requestSpecification -> requestSpecification.header(AUTHORIZATION_HEADER_NAME, "Bearer %s".formatted(parishKey)));
    }

    /**
     * Использовать при отправке запросов авторизацию через api-токен
     * пример использования
     * <p>
     * ИСПОЛЬЗУЕТСЯ В БЛОКЕ Предыстория ДЛЯ ИСПОЛЬЗОВАНИЯ В НЕСКОЛЬКИХ СЦЕНАРИЯХ В ОДНОМ ФАЙЛЕ
     * <p>
     * Предыстория:
     * __Допустим для авторизации используется api-токен
     */
    @And("^для авторизации используется api-токен$")
    public void authorizeWithXApiKeyHeader() {
        httpRequestData.setAuthorizationFunction(requestSpecification -> requestSpecification.header(API_KEY_HEADER_NAME, TEST_API_KEY));
    }

    /**
     * Использовать при отправке запросов авторизацию через внутренний api-токен
     * пример использования
     * <p>
     * ИСПОЛЬЗУЕТСЯ В БЛОКЕ Предыстория ДЛЯ ИСПОЛЬЗОВАНИЯ В НЕСКОЛЬКИХ СЦЕНАРИЯХ В ОДНОМ ФАЙЛЕ
     * <p>
     * Предыстория:
     * __Допустим для авторизации используется внутренний api-токен
     */
    @And("^для авторизации используется внутренний api-токен$")
    public void authorizeWithInternalXApiKeyHeader() {
        httpRequestData.setAuthorizationFunction(requestSpecification -> requestSpecification.header(API_KEY_HEADER_NAME, INTERNAL_TEST_API_KEY));
    }

    /**
     * Начало формирования запроса
     * пример использования
     * <p>
     * Когда подготавливаем запрос Создание города
     *
     * @param endpoint указанный эндпоинт
     */
    @When("^подготавливаем запрос (.+)$")
    public void startBuildingRequestSpecificationParams(ImshabyApiEndpoint endpoint) {
        cleanUp();

        httpRequestData.setEndpoint(endpoint);
    }

    /**
     * Сохранить переменные запроса
     * пример использования
     * <p>
     * И с переменными
     * | переменная1 | значение1 |
     * | переменная2 | значение2 |
     *
     * @param dataTable таблица с переменными
     */
    @And("^с переменными$")
    public void withVariableValues(final DataTable dataTable) {
        if (dataTable.width() != 2) {
            throw new IllegalArgumentException("Таблица должна содержать 2 колонки, название переменной и значение");
        }
        httpRequestData.setPathVariables(dataTable.cells().stream()
                .collect(
                        Collectors.toMap(
                                item -> item.get(0),//0 - первая колонка, название переменной
                                item -> globalStorage.tryResolveModifier(item.get(1))//1 - вторая колонка, значение параметра
                        )
                )
        );
    }

    /**
     * Сохранить параметры запроса
     * пример использования
     * <p>
     * И с параметрами
     * | параметр1 | значение1 |
     * | параметр2 | значение2 |
     *
     * @param dataTable таблица с параметрами
     */
    @And("^с параметрами$")
    public void withRequestParams(final DataTable dataTable) {
        if (dataTable.width() != 2) {
            throw new IllegalArgumentException("Таблица должна содержать 2 колонки, название параметра и значение");
        }
        httpRequestData.setRequestParams(dataTable.cells().stream()
                .collect(
                        Collectors.toMap(
                                item -> item.get(0),//0 - первая колонка, название параметра
                                item -> globalStorage.tryResolveModifier(item.get(1))//1 - вторая колонка, значение параметра
                        )
                )
        );
    }

    /**
     * Сохранить заголовки запроса
     * пример использования
     * <p>
     * И с заголовками
     * | заголовок1 | значение1 |
     * | заголовок2 | значение2 |
     *
     * @param dataTable таблица с параметрами
     */
    @And("^с заголовками$")
    public void withRequestHeaders(final DataTable dataTable) {
        if (dataTable.width() != 2) {
            throw new IllegalArgumentException("Таблица должна содержать 2 колонки, название заголовка и значение");
        }
        httpRequestData.setRequestHeaders(dataTable.cells().stream()
                .collect(
                        Collectors.toMap(
                                item -> item.get(0),//0 - первая колонка, название заголовка
                                item -> globalStorage.tryResolveModifier(item.get(1))//1 - вторая колонка, значение заголовка
                        )
                )
        );
    }

    /**
     * Сохранить тело запроса
     * пример использования
     * <p>
     * И телом запроса
     * """
     * {
     * "field1": "value1"
     * }
     * """
     *
     * @param requestAsJson тело запроса в формате json
     */
    @And("^телом запроса$")
    public void withRequestBody(final DocString requestAsJson) {
        try {
            final String content = requestAsJson.getContent();
            objectMapper.readTree(content);
            httpRequestData.setRequestBody(content);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("тело запроса должно быть в формате json");
        }
    }

    /**
     * Выполнить запрос
     * пример использования
     * <p>
     * Тогда выполняем запрос
     */
    @And("^выполняем запрос$")
    public void sendRequest() throws JsonProcessingException {
        RequestSpecification specification = given();

        if (httpRequestData.getPathVariables() != null) {
            specification = specification.pathParams(httpRequestData.getPathVariables());
        }
        if (httpRequestData.getRequestParams() != null) {
            specification = specification.params(httpRequestData.getEndpoint().validateAndGetRequestParams(httpRequestData.getRequestParams()));
        }
        if (httpRequestData.getRequestHeaders() != null) {
            specification = specification.headers(httpRequestData.getRequestHeaders());
        }
        if (httpRequestData.getRequestBody() != null) {
            specification = specification.body(httpRequestData.getRequestBody());
        }
        if (!httpRequestData.getRequestBodyModifiers().isEmpty()) {
            ObjectNode body = (ObjectNode) objectMapper.readTree("{}");
            //если тело было заполнено, то модификаторы применяются поверх него (возможно когда-то будет полезно)
            if (httpRequestData.getRequestBody() != null) {
                body = (ObjectNode) objectMapper.readTree(httpRequestData.getRequestBody());
            }

            for (RequestBodyModifier requestBodyModifier : httpRequestData.getRequestBodyModifiers()) {
                body = requestBodyModifier.apply(body);
            }

            specification = specification.body(body);
        }
        if (httpRequestData.getAuthorizationFunction() != null) {
            specification = httpRequestData.getAuthorizationFunction().apply(specification);
        }

        httpResponseHolder.setValidatableResponse(
                specification.
                        request(httpRequestData.getEndpoint().getMethod(), httpRequestData.getEndpoint().getPath()).
                        then()
        );
        httpResponseHolder.setImshabyApiEndpoint(httpRequestData.getEndpoint());
    }

    private void cleanUp() {
        httpRequestData.cleanUp();
        httpResponseHolder.cleanUp();
    }
}
