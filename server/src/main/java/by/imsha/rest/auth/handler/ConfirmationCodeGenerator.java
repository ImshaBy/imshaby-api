package by.imsha.rest.auth.handler;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConfirmationCodeGenerator {

    /**
     * Генерация кода
     *
     * @param key ключ для кода
     * @return сгенерированный код (каждый вызов метода порождает новый код)
     */
    @CachePut(cacheNames = "confirmationCodeCache")
    public synchronized String generate(String key) {
        return RandomStringUtils.randomNumeric(4);
    }

    /**
     * Получение сгенерированного кода
     *
     * @param key ключ для кода
     * @return сгенерированный ранее код для ключа либо {@link Optional#empty()}
     */
    @Cacheable(cacheNames = "confirmationCodeCache")
    public Optional<String> getCode(String key) {
        return Optional.empty();
    }
}
