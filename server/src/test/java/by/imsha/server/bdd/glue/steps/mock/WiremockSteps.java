package by.imsha.server.bdd.glue.steps.mock;

import com.github.tomakehurst.wiremock.admin.model.ServeEventQuery;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import org.assertj.core.api.Assertions;

public class WiremockSteps {

    /**
     * Сохранить переменные запроса
     * пример использования
     * <p>
     * Дано создана заглушка *Имя заглушки*
     * | параметр1 | значение1 |
     * | параметр2 | значение2 |
     *
     * @param dataTable таблица с переменными
     */
    @And("создана заглушка {string}")
    public void createStub(String wireMockStubName, DataTable dataTable) {
        WireMock.stubFor(WireMockStub.getByName(wireMockStubName).getMapping(dataTable));
    }

    @And("все заглушки сработали")
    public void allStubsWereCalled() {

        WireMock.listAllStubMappings()
                .getMappings()
                .forEach(stubMapping -> {
                    int stubCalls = WireMock.getAllServeEvents(ServeEventQuery.forStubMapping(stubMapping)).size();
                    Assertions.assertThat(stubCalls).isEqualTo(1);
                });
    }
}
