package by.imsha.validation.mass;

import by.imsha.domain.Mass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TODO можно покрыть отдельно тестами, все зависимости не статические
 */
class UniqueMassValidatorTest {

    @Test
    void testMassHasFieldNamedTime() {
        final BeanWrapperImpl massWrapper = new BeanWrapperImpl(new Mass());

        assertThat(massWrapper.isReadableProperty(UniqueMassValidator.TIME_FIELD_NAME)).isTrue();
    }
}
