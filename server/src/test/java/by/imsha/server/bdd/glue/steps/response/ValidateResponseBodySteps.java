package by.imsha.server.bdd.glue.steps.response;

import by.imsha.server.bdd.glue.components.GlobalStorage;
import by.imsha.server.bdd.glue.steps.HttpResponseHolder;
import by.imsha.server.bdd.glue.steps.request.HttpRequestData;
import io.cucumber.java.en.Then;
import io.restassured.internal.ValidatableResponseImpl;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class ValidateResponseBodySteps {

    @Autowired
    private HttpRequestData httpRequestData;
    @Autowired
    private HttpResponseHolder httpResponseHolder;
    @Autowired
    private GlobalStorage globalStorage;
    /**
     * Проверить поле на соответствие набору 24 символов
     * пример использования
     * <p>
     * Тогда поле "Идентификатор" заполнено последовательностью 24 символов
     */
    @Then("^полученное поле \"(.+)\" заполнено последовательностью 24 символов$")
    public void checkResponseStatusCode(String fieldQualifier) {
        String fieldName = httpRequestData.getEndpoint().getResponseFieldNameGetter().getFieldName(fieldQualifier);
        httpResponseHolder.getValidatableResponse().body(fieldName, Matchers.matchesRegex("[0-9|a-f]{24}"));
    }

    /**
     * пример использования
     * <p>
     * Тогда полученное поле "Идентификатор" заполнено значением "значение1"
     */
    @Then("^полученное поле \"(.+)\" заполнено значением \"(.+)\"$")
    public void checkResponseNameField(String fieldQualifier, String value) {
        String fieldName = httpRequestData.getEndpoint().getResponseFieldNameGetter().getFieldName(fieldQualifier);
        String fieldValue = ((ValidatableResponseImpl) httpResponseHolder.getValidatableResponse()).originalResponse().jsonPath().getString(fieldName);
        if (value.equals("null")) {
            assertThat(fieldValue, Matchers.nullValue());
        } else {
            assertThat(fieldValue, Matchers.equalTo(globalStorage.tryResolveModifier(value)));
        }
    }

    @Then("^полученное расписание содержит Службы 06/25/2024 23:59, 06/25/2024 23:59 и 06/26/2024 20:00$")
    public void checkResponseWeekWithDates() {
        httpResponseHolder.getValidatableResponse().assertThat()
                .body("$", Matchers.hasKey("startWeekDate"))
                .body("$", Matchers.hasKey("nav"))
                .body("nav", Matchers.hasKey("selected"))
                .body("nav", Matchers.hasKey("guided"))
                .body("$", Matchers.hasKey("schedule"))
                .body("schedule[0].date", Matchers.equalTo("06/24/2024"))
                .body("schedule[0].massHours[0].data[0].id", Matchers.equalTo(globalStorage.get("getMass_id")))
                .body("schedule[0].massHours[0].hour", Matchers.equalTo("23:59"))
                .body("schedule[0].massHours[0].data[0].days", Matchers.hasItem(1))
                .body("schedule[1].date", Matchers.equalTo("06/25/2024"))
                .body("schedule[1].massHours[0].data[0].id", Matchers.equalTo(globalStorage.get("getMass_id")))
                .body("schedule[1].massHours[0].hour", Matchers.equalTo("23:59"))
                .body("schedule[0].massHours[0].data[0].days", Matchers.hasItem(2))
                .body("schedule[2].date", Matchers.equalTo("06/26/2024"))
                .body("schedule[2].massHours[0].data[0].id", Matchers.equalTo(globalStorage.get("getMass2_id")))
                .body("schedule[2].massHours[0].hour", Matchers.equalTo("20:00"))
                .body("schedule[2].massHours[0].data[0].days", Matchers.hasItem(3));

    }

    @Then("^полученное расписание валидное и не имеет Служб$")
    public void checkResponseEmptyWeek() {
        httpResponseHolder.getValidatableResponse().assertThat()
                .body("$", Matchers.hasKey("startWeekDate"))
                .body("$", Matchers.hasKey("nav"))
                .body("nav", Matchers.hasKey("selected"))
                .body("nav", Matchers.hasKey("guided"))
                .body("$", Matchers.hasKey("schedule"));
    }

    /**
     * пример использования
     * <p>
     * Тогда полученный ответ является массивом с размером "значение1"
     */
    @Then("^полученный ответ является массивом с размером \"(.+)\"$")
    public void checkResponseArraySize(int size) {
        httpResponseHolder.getValidatableResponse().body("", Matchers.hasSize(size));
    }

    /**
     * пример использования
     * <p>
     * Тогда полученное поле "Аrray" является массивом с размером "значение1"
     */
    @Then("^полученное поле \"(.+)\" является массивом с размером \"(.+)\"$")
    public void checkResponseArraySize(String fieldQualifier, int size) {
        String fieldName = httpRequestData.getEndpoint().getResponseFieldNameGetter().getFieldName(fieldQualifier);
        httpResponseHolder.getValidatableResponse().body(fieldName, Matchers.hasSize(size));
    }

    /**
     * пример использования
     * <p>
     * Тогда полученный массив "Аrray" содержит в себе значение "значение1"
     */
    @Then("^полученный массив \"(.+)\" содержит в себе значение \"(.+)\"$")
    public void checkResponseArray(String fieldQualifier, String value) {
        String fieldName = httpRequestData.getEndpoint().getResponseFieldNameGetter().getFieldName(fieldQualifier);
        List<String> array = ((ValidatableResponseImpl) httpResponseHolder.getValidatableResponse()).originalResponse().jsonPath().getList(fieldName, String.class);
        assertThat(array, Matchers.hasItem((String) globalStorage.tryResolveModifier(value)));
    }

    /**
     * пример использования
     * <p>
     * Тогда полученное поле "Дата" является датой в формате значение1
     */
    @Then("^полученное поле \"(.+)\" является датой в формате \"([^\"]+)\"$")
    public void checkResponseDate(String fieldQualifier, String dateFormat) {
        String fieldName = httpRequestData.getEndpoint().getResponseFieldNameGetter().getFieldName(fieldQualifier);
        String fieldValue = ((ValidatableResponseImpl) httpResponseHolder.getValidatableResponse()).originalResponse().jsonPath().getString(fieldName);
        assertThat("Дата не соответствует ожидаемому формату", isValidDate(fieldValue, dateFormat), Matchers.equalTo(true));
    }

    /**
     * пример использования
     * <p>
     * Тогда полученное поле "Дата" является датой в формате значение1 и больше значение2
     */
    @Then("^полученное поле \"(.+)\" является датой в формате \"([^\"]+)\" и больше \"(.+)\"$")
    public void checkResponseDateIsBefore(String fieldQualifier, String dateFormat, String firstDate) {
        String fieldName = httpRequestData.getEndpoint().getResponseFieldNameGetter().getFieldName(fieldQualifier);
        String secondDate = ((ValidatableResponseImpl) httpResponseHolder.getValidatableResponse()).originalResponse().jsonPath().getString(fieldName);
        firstDate = globalStorage.tryResolveModifier(firstDate);
        assertThat("Одна или обе даты не соответствуют ожидаемому формату",
                isValidDate(firstDate, dateFormat) && isValidDate(secondDate, dateFormat), Matchers.equalTo(true));
        assertThat("Первая дата не меньше второй",
                parseDate(firstDate, dateFormat).isBefore(parseDate(secondDate, dateFormat)), Matchers.equalTo(true));
    }

    private boolean isValidDate(String dateString, String dateFormat) {
        try {
            parseDate(dateString, dateFormat);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private LocalDateTime parseDate(String dateString, String dateFormat) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        return LocalDateTime.parse(dateString, formatter);
    }
}