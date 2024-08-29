package by.imsha.server.bdd.glue.steps.response;

import by.imsha.server.bdd.glue.components.GlobalStorage;
import by.imsha.server.bdd.glue.steps.HttpResponseHolder;
import by.imsha.server.bdd.glue.steps.request.HttpRequestData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

public class ResponseBodyStoreSteps {

    @Autowired
    private HttpResponseHolder httpResponseHolder;
    @Autowired
    private HttpRequestData httpRequestData;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GlobalStorage globalStorage;

    /**
     * Сохранить значение поля из ответа
     * пример использования
     * <p>
     * И сохранить "Идентификатор" из ответа с ключом QWERTY
     */
    @Then("^сохранить \"(.*)\" из ответа с ключом (.*)$")
    public void checkResponseStatusCode(final String fieldQualifier, final String key) throws JsonProcessingException {
        String fieldName = httpRequestData.getEndpoint().getResponseFieldNameGetter().getFieldName(fieldQualifier);
        globalStorage.put(key, objectMapper.readTree(httpResponseHolder.getValidatableResponse().extract().body().asString())
                .get(fieldName).textValue());
    }
}
