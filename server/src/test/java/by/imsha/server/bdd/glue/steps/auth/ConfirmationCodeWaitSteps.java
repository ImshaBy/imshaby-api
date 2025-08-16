package by.imsha.server.bdd.glue.steps.auth;

import by.imsha.rest.auth.handler.ConfirmationCodeGenerator;
import by.imsha.server.bdd.glue.components.GlobalStorage;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class ConfirmationCodeWaitSteps {

    @Autowired
    private GlobalStorage globalStorage;

    @Autowired
    private ConfirmationCodeGenerator confirmationCodeGenerator;

    /**
     * Сохранить переменные запроса
     * пример использования
     * <p>
     * И создан код подтверждения для почты tesd@mail.com и сохранен с ключом Код подтверждения
     */
    @And("создан код подтверждения для почты {string} и сохранен с ключом {string}")
    public void createStub(String email, String key) {
        for (int i = 0; i < 5; i++) {
            Optional<String> code = confirmationCodeGenerator.getCode(email);
            if (code.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }

            globalStorage.put(key, code.get());
            return;
        }

        throw new RuntimeException("Код подтверждения не был получен за 5 сек");
    }
}
