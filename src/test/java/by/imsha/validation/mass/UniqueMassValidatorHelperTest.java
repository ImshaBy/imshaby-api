package by.imsha.validation.mass;

import by.imsha.domain.Mass;
import by.imsha.validation.mass.UniqueMassValidatorHelper.CommonPeriodCalculator;
import by.imsha.validation.mass.UniqueMassValidatorHelper.MassPeriodType;
import by.imsha.validation.mass.UniqueMassValidatorHelper.Period;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static by.imsha.validation.mass.UniqueMassValidatorHelper.MassPeriodType.BOUNDED;
import static by.imsha.validation.mass.UniqueMassValidatorHelper.MassPeriodType.BOUNDED_ABOVE;
import static by.imsha.validation.mass.UniqueMassValidatorHelper.MassPeriodType.BOUNDED_BELOW;
import static by.imsha.validation.mass.UniqueMassValidatorHelper.MassPeriodType.INFINITE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class UniqueMassValidatorHelperTest {

    private static final DateTimeFormatter LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @MethodSource("getMassPeriodTypeTestParameters")
    @ParameterizedTest
    void testGetMassPeriodType(final LocalDate startDate, final LocalDate endDate, final MassPeriodType expectedMassPeriodType) {
        final UniqueMassValidatorHelper helper = new UniqueMassValidatorHelper();

        final Mass mass = new Mass();
        mass.setStartDate(startDate);
        mass.setEndDate(endDate);

        final MassPeriodType massPeriodType = helper.getMassPeriodType(mass);

        assertThat(massPeriodType).isEqualTo(expectedMassPeriodType);
    }

    @MethodSource("allVariantsTestParameters")
    @ParameterizedTest
    void testAllVariants(
            final MassPeriodType firstPeriodTypeExpected,
            final MassPeriodType secondPeriodTypeExpected,
            final String firstStartDateStr,
            final String firstEndDateStr,
            final String secondStartDateStr,
            final String secondEndDateStr,
            final String expectedCommonPeriodStr,
            final boolean periodValid
    ) {
        final LocalDate firstStartDate = toDateIfNotNull(firstStartDateStr);
        final LocalDate firstEndDate = toDateIfNotNull(firstEndDateStr);
        final LocalDate secondStartDate = toDateIfNotNull(secondStartDateStr);
        final LocalDate secondEndDate = toDateIfNotNull(secondEndDateStr);
        final String[] periodParts = expectedCommonPeriodStr.split(" - ");
        final Period expectedCommonPeriod = new Period(
                LocalDate.parse(periodParts[0], LOCAL_DATE_FORMATTER),
                LocalDate.parse(periodParts[1], LOCAL_DATE_FORMATTER)
        );

        final Mass firstMass = new Mass();
        firstMass.setStartDate(firstStartDate);
        firstMass.setEndDate(firstEndDate);
        final Mass secondMass = new Mass();
        secondMass.setStartDate(secondStartDate);
        secondMass.setEndDate(secondEndDate);

        final UniqueMassValidatorHelper helper = new UniqueMassValidatorHelper();

        final MassPeriodType firstPeriodType = helper.getMassPeriodType(firstMass);
        final MassPeriodType secondPeriodType = helper.getMassPeriodType(secondMass);

        final CommonPeriodCalculator calculator = helper.getCalculatorMapForPeriodType(firstPeriodType).get(secondPeriodType);

        final Period commonPeriod = calculator.getCommonPeriod(firstStartDate, firstEndDate, secondStartDate, secondEndDate);

        assertAll(
                () -> assertThat(firstPeriodTypeExpected).isEqualTo(firstPeriodType),
                () -> assertThat(secondPeriodTypeExpected).isEqualTo(secondPeriodType),
                () -> assertThat(commonPeriod.isInvalid()).isEqualTo(!periodValid),
                () -> assertThat(expectedCommonPeriod.isInvalid()).isEqualTo(!periodValid),
                () -> assertThat(commonPeriod.getStartDate()).isEqualTo(expectedCommonPeriod.getStartDate()),
                () -> assertThat(commonPeriod.getEndDate()).isEqualTo(expectedCommonPeriod.getEndDate())
        );
    }

    private static Stream<Arguments> getMassPeriodTypeTestParameters() {
        return Stream.of(
                Arguments.of(null, null, INFINITE),
                Arguments.of(null, LocalDate.now(), BOUNDED_ABOVE),
                Arguments.of(LocalDate.now(), null, BOUNDED_BELOW),
                Arguments.of(LocalDate.now(), LocalDate.now(), BOUNDED)
        );
    }

    private static Stream<Arguments> allVariantsTestParameters() {
        return Stream.of(
                //BOUNDED, BOUNDED
                Arguments.of(BOUNDED, BOUNDED, "01-01-2023", "01-02-2023", "01-01-2023", "01-02-2023", "01-01-2023 - 01-02-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED, "01-01-2023", "01-02-2023", "03-01-2023", "20-01-2023", "03-01-2023 - 20-01-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED, "03-01-2023", "20-01-2023", "01-01-2023", "01-02-2023", "03-01-2023 - 20-01-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED, "15-01-2023", "30-01-2023", "01-01-2023", "20-01-2023", "15-01-2023 - 20-01-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED, "01-01-2023", "20-01-2023", "03-01-2023", "20-01-2023", "03-01-2023 - 20-01-2023", TRUE),
                //BOUNDED, BOUNDED_BELOW
                Arguments.of(BOUNDED, BOUNDED_BELOW, "01-01-2023", "01-02-2023", "01-01-2023", null, "01-01-2023 - 01-02-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED_BELOW, "01-01-2023", "01-02-2023", "15-01-2023", null, "15-01-2023 - 01-02-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED_BELOW, "01-01-2023", "01-02-2023", "25-12-2022", null, "01-01-2023 - 01-02-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED_BELOW, "01-01-2023", "01-02-2023", "25-01-2023", null, "25-01-2023 - 01-02-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED_BELOW, "01-01-2023", "01-02-2023", "25-02-2023", null, "25-02-2023 - 01-02-2023", FALSE),
                //BOUNDED, BOUNDED_ABOVE
                Arguments.of(BOUNDED, BOUNDED_ABOVE, "01-01-2023", "01-02-2023", null, "01-01-2023", "01-01-2023 - 01-01-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED_ABOVE, "01-01-2023", "01-02-2023", null, "05-01-2023", "01-01-2023 - 05-01-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED_ABOVE, "01-01-2023", "01-02-2023", null, "10-01-2023", "01-01-2023 - 10-01-2023", TRUE),
                Arguments.of(BOUNDED, BOUNDED_ABOVE, "01-01-2023", "01-02-2023", null, "01-12-2022", "01-01-2023 - 01-12-2022", FALSE),
                //BOUNDED, INFINITE
                Arguments.of(BOUNDED, INFINITE, "01-01-2023", "01-02-2023", null, null, "01-01-2023 - 01-02-2023", TRUE),
                Arguments.of(BOUNDED, INFINITE, "01-01-2023", "01-01-2022", null, null, "01-01-2023 - 01-01-2022", FALSE),
                //BOUNDED_BELOW, BOUNDED
                Arguments.of(BOUNDED_BELOW, BOUNDED, "01-01-2023", null, "01-01-2023", "01-02-2023", "01-01-2023 - 01-02-2023", TRUE),
                Arguments.of(BOUNDED_BELOW, BOUNDED, "15-01-2023", null, "01-01-2023", "01-02-2023", "15-01-2023 - 01-02-2023", TRUE),
                Arguments.of(BOUNDED_BELOW, BOUNDED, "25-12-2022", null, "01-01-2023", "01-02-2023", "01-01-2023 - 01-02-2023", TRUE),
                Arguments.of(BOUNDED_BELOW, BOUNDED, "25-01-2023", null, "01-01-2023", "01-02-2023", "25-01-2023 - 01-02-2023", TRUE),
                Arguments.of(BOUNDED_BELOW, BOUNDED, "25-02-2023", null, "01-01-2023", "01-02-2023", "25-02-2023 - 01-02-2023", FALSE),
                //BOUNDED_BELOW, BOUNDED_BELOW
                Arguments.of(BOUNDED_BELOW, BOUNDED_BELOW, "01-01-2023", null, "01-01-2023", null, "01-01-2023 - 08-01-2023", TRUE),
                Arguments.of(BOUNDED_BELOW, BOUNDED_BELOW, "01-01-2023", null, "04-01-2023", null, "04-01-2023 - 11-01-2023", TRUE),
                Arguments.of(BOUNDED_BELOW, BOUNDED_BELOW, "05-01-2023", null, "01-01-2023", null, "05-01-2023 - 12-01-2023", TRUE),
                //BOUNDED_BELOW, BOUNDED_ABOVE
                Arguments.of(BOUNDED_BELOW, BOUNDED_ABOVE, "05-01-2023", null, null, "01-01-2023", "05-01-2023 - 01-01-2023", FALSE),
                Arguments.of(BOUNDED_BELOW, BOUNDED_ABOVE, "01-01-2023", null, null, "05-01-2023", "01-01-2023 - 05-01-2023", TRUE),
                Arguments.of(BOUNDED_BELOW, BOUNDED_ABOVE, "01-01-2023", null, null, "10-01-2023", "01-01-2023 - 10-01-2023", TRUE),
                //BOUNDED_BELOW, INFINITE
                Arguments.of(BOUNDED_BELOW, INFINITE, "01-01-2023", null, null, null, "01-01-2023 - 08-01-2023", TRUE),
                Arguments.of(BOUNDED_BELOW, INFINITE, "30-01-2023", null, null, null, "30-01-2023 - 06-02-2023", TRUE),
                //BOUNDED_ABOVE, BOUNDED
                Arguments.of(BOUNDED_ABOVE, BOUNDED, null, "01-01-2023", "01-01-2023", "01-02-2023", "01-01-2023 - 01-01-2023", TRUE),
                Arguments.of(BOUNDED_ABOVE, BOUNDED, null, "05-01-2023", "01-01-2023", "01-02-2023", "01-01-2023 - 05-01-2023", TRUE),
                Arguments.of(BOUNDED_ABOVE, BOUNDED, null, "10-01-2023", "01-01-2023", "01-02-2023", "01-01-2023 - 10-01-2023", TRUE),
                Arguments.of(BOUNDED_ABOVE, BOUNDED, null, "01-12-2022", "01-01-2023", "01-02-2023", "01-01-2023 - 01-12-2022", FALSE),
                //BOUNDED_ABOVE, BOUNDED_BELOW
                Arguments.of(BOUNDED_ABOVE, BOUNDED_BELOW, null, "01-01-2023", "05-01-2023", null, "05-01-2023 - 01-01-2023", FALSE),
                Arguments.of(BOUNDED_ABOVE, BOUNDED_BELOW, null, "05-01-2023", "01-01-2023", null, "01-01-2023 - 05-01-2023", TRUE),
                Arguments.of(BOUNDED_ABOVE, BOUNDED_BELOW, null, "10-01-2023", "01-01-2023", null, "01-01-2023 - 10-01-2023", TRUE),
                //BOUNDED_ABOVE, BOUNDED_ABOVE
                Arguments.of(BOUNDED_ABOVE, BOUNDED_ABOVE, null, "01-01-2023", null, "01-01-2023", "01-01-1970 - 01-01-2023", TRUE),
                Arguments.of(BOUNDED_ABOVE, BOUNDED_ABOVE, null, "01-01-2023", null, "10-01-2023", "01-01-1970 - 01-01-2023", TRUE),
                Arguments.of(BOUNDED_ABOVE, BOUNDED_ABOVE, null, "10-01-2023", null, "01-01-2023", "01-01-1970 - 01-01-2023", TRUE),
                Arguments.of(BOUNDED_ABOVE, BOUNDED_ABOVE, null, "03-01-1970", null, "01-01-2023", "01-01-1970 - 03-01-1970", TRUE),
                Arguments.of(BOUNDED_ABOVE, BOUNDED_ABOVE, null, "01-01-2023", null, "03-01-1970", "01-01-1970 - 03-01-1970", TRUE),
                //BOUNDED_ABOVE, INFINITE
                Arguments.of(BOUNDED_ABOVE, INFINITE, null, "01-01-2023", null, null, "01-01-1970 - 01-01-2023", TRUE),
                Arguments.of(BOUNDED_ABOVE, INFINITE, null, "08-12-2023", null, null, "01-01-1970 - 08-12-2023", TRUE),
                //INFINITE, BOUNDED
                Arguments.of(INFINITE, BOUNDED, null, null, "01-01-2023", "01-02-2023", "01-01-2023 - 01-02-2023", TRUE),
                Arguments.of(INFINITE, BOUNDED, null, null, "01-01-2023", "01-01-2022", "01-01-2023 - 01-01-2022", FALSE),
                //INFINITE, BOUNDED_BELOW
                Arguments.of(INFINITE, BOUNDED_BELOW, null, null, "01-01-2023", null, "01-01-2023 - 08-01-2023", TRUE),
                Arguments.of(INFINITE, BOUNDED_BELOW, null, null, "30-01-2023", null, "30-01-2023 - 06-02-2023", TRUE),
                //INFINITE, BOUNDED_ABOVE
                Arguments.of(INFINITE, BOUNDED_ABOVE, null, null, null, "01-01-2023", "01-01-1970 - 01-01-2023", TRUE),
                Arguments.of(INFINITE, BOUNDED_ABOVE, null, null, null, "08-12-2023", "01-01-1970 - 08-12-2023", TRUE),
                //INFINITE, INFINITE
                Arguments.of(INFINITE, INFINITE, null, null, null, null, "01-01-1970 - 08-01-1970", TRUE)
        );
    }

    private LocalDate toDateIfNotNull(final String dateStr) {
        return dateStr == null ? null : LocalDate.parse(dateStr, LOCAL_DATE_FORMATTER);
    }
}
