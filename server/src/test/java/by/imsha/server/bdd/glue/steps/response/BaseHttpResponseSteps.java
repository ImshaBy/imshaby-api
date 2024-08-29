package by.imsha.server.bdd.glue.steps.response;

import by.imsha.server.bdd.glue.steps.HttpResponseHolder;
import io.cucumber.java.en.Then;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;

public class BaseHttpResponseSteps {

    @Autowired
    private HttpResponseHolder httpResponseHolder;

    /**
     * Проверить код ответа
     * пример использования
     * <p>
     * Тогда код ответа равен 200
     */
    @Then("^код ответа равен (\\d+)$")
    public void checkResponseStatusCode(final Integer statusCode) {
        httpResponseHolder.getValidatableResponse().statusCode(statusCode);
    }

    @Then("^в ответе содержится время ошибки$")
    public void checkErrorResponseTimestamp() {
        httpResponseHolder.getValidatableResponse().body("timestamp", new CustomMatcher<>("ISO with offset date time format expected") {
            @Override
            public boolean matches(Object actual) {
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse((String) actual);
                return true;
            }
        });
    }

    @Then("^в ответе содержится ошибка с кодом (.*)$")
    public void checkErrorResponseCode(String errorCode) {
        httpResponseHolder.getValidatableResponse()
                .body("errors", Matchers.hasItem(
                        Matchers.hasEntry("code", errorCode))
                );
    }

    /**
     * пример использования
     * <p>
     * Тогда сделать паузу в 1 секунду
     */
    @Then("^сделать паузу в 1 секунду$")
    public void pause() throws InterruptedException {
            Thread.sleep(1000);
    }
}
