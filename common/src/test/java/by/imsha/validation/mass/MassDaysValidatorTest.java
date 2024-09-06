package by.imsha.validation.mass;

import by.imsha.domain.Mass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TODO покрыть тестами остальное
 */
class MassDaysValidatorTest {

    @Test
    void testMassHasFieldNamedDays() {
        final BeanWrapperImpl massWrapper = new BeanWrapperImpl(new Mass());

        assertThat(massWrapper.isReadableProperty(MassDaysValidator.DAYS_FIELD_NAME)).isTrue();
    }
}
