package by.imsha.validation.mass;

import by.imsha.domain.Mass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест только на проверку наименования поля, т.к. все остальные проверки в тестах контроллера
 *
 * @see by.imsha.rest.MassControllerCreateMassTest
 */
class UniqueMassValidatorTest {

    @Test
    void testMassHasFieldNamedTime() {
        final BeanWrapperImpl massWrapper = new BeanWrapperImpl(new Mass());

        assertThat(massWrapper.isReadableProperty(UniqueMassValidator.TIME_FIELD_NAME)).isTrue();
    }
}
