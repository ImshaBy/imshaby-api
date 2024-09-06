package by.imsha.server.bdd.glue.steps.request;

import by.imsha.server.bdd.glue.components.GlobalStorage;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

public class ModifyRequestBodySteps {

    @Autowired
    private HttpRequestData httpRequestData;
    @Autowired
    private GlobalStorage globalStorage;

    @When("^поле \"(.+)\" заполнено значением \"(.+)\"$")
    public void addNameField(String name, String value) {
        httpRequestData.addRequestBodyModifier(jsonNode -> httpRequestData.getEndpoint().getFieldValueSetter().apply(jsonNode, name, globalStorage.tryResolveModifier(value)));
    }

    @When("^поле \"(.+)\" заполнено уникальным значением")
    public void addNameFieldWithRandomUUID(String name) {
        httpRequestData.addRequestBodyModifier(jsonNode -> httpRequestData.getEndpoint().getFieldValueSetter().apply(jsonNode, name,
                UUID.randomUUID().toString().replace("-", "").substring(0, 24)));
    }

    @When("^поле \"(.+)\" заполнено уникальным значением и сохранено с ключом \"(.*)\"")
    public void addNameFieldWithRandomUuidAndSave(String name, String key) {
        String randomUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        httpRequestData.addRequestBodyModifier(jsonNode -> httpRequestData.getEndpoint().getFieldValueSetter().apply(jsonNode, name, randomUuid));
        globalStorage.put(key, randomUuid);
    }
}
