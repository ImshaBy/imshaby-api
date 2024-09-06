package by.imsha.server.bdd.glue;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

public class Hooks {
    private static String CURRENT_URI = StringUtils.EMPTY;

    @LocalServerPort
    private Integer serverPort;

    @Autowired
    private RestTemplate internalRestTemplate;

    @Before
    public void setUp(Scenario scenario) {
        if (!CURRENT_URI.equals(scenario.getUri().toString())) {
            CURRENT_URI = scenario.getUri().toString();
            MongoDBHelper mongoDBHelper = new MongoDBHelper();
            mongoDBHelper.cleanDatabase();
            mongoDBHelper.close();

            internalRestTemplate.exchange(
                            RequestEntity.post(
                                    "http://localhost:" + serverPort + "/api/cache/clear"
                                    ).build(),
                            Void.class
                    );
        }
    }
}