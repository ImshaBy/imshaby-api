package by.imsha.server.bdd.glue;

import by.imsha.Application;
import by.imsha.server.ImshabyApiEndpoint;
import by.imsha.server.properties.ImshabyApiTestProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cucumber.java.ParameterType;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * для использования удобств spring-boot и cucumber-spring необходимо
 * объявить @CucumberContextConfiguration и конфигурацию контекста
 * (в нашем случае просто @SpringBootTest, это означает, что будет приложение будет загружено полностью)
 * <p>
 * + конфигурация одна на все тесты, а для помощи есть дополнительный scope в cucumber-spring
 *
 * @see io.cucumber.spring.CucumberTestContext#SCOPE_CUCUMBER_GLUE
 * @see io.cucumber.spring.ScenarioScope
 */
@CucumberContextConfiguration
@ActiveProfiles({"test"})
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringGlue {

    public static final int MONGO_PORT;

    public static final WireMockServer wireMockServer;

    static {

        //тестовые контейнеры удаляются после тестов
        GenericContainer mongoContainer = new GenericContainer("mongo:latest")
                .withExposedPorts(27017);
        mongoContainer.start();
        MONGO_PORT = mongoContainer.getMappedPort(27017);

        wireMockServer = new WireMockServer(
                WireMockConfiguration.options()
                        .globalTemplating(true)
                        .templatingEnabled(true)
                        .dynamicPort()
        );
        wireMockServer.start();
        WireMock.configureFor(new WireMock(wireMockServer));
    }

    @DynamicPropertySource
    static void registerImshabyApiPort(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:" + MONGO_PORT + "/imshaby");
        registry.add("wiremock.baseUrl", wireMockServer::baseUrl);
    }

    @LocalServerPort
    private Integer serverPort;

    /**
     * Для использования ImshabyApiEndpoints в методах регистрируем конвертер для типа
     *
     * @param cyrillicCode код эндпоинта на русском (в кавычках, напр. "Создание города")
     * @return значение enum, соответствующее коду
     */
    @ParameterType(".+")
    public ImshabyApiEndpoint endpoint(final String cyrillicCode) {
        return ImshabyApiEndpoint.fromCyrillicCode(cyrillicCode);
    }

    @Autowired
    private ImshabyApiTestProperties properties;

    /**
     * Настройка статики RestAssured
     */
    @PostConstruct
    public void restAssuredSetup() {
        RestAssured.baseURI = "http://localhost:" + serverPort;
        RestAssured.basePath = properties.basePath();
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON.withCharset(UTF_8))
                .setAccept(ContentType.JSON)
                .build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
