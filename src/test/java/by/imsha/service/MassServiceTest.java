package by.imsha.service;

import by.imsha.domain.Mass;
import by.imsha.utils.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MassServiceTest {

    private static MassService massService;

//    public static String DATE_FORMAT = "MM/dd/yyyy";


    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);


    @BeforeAll
    public static void setUp() {
        // todo: check how to initiate service in test for Spring
        massService = new MassService();
    }

    @Test
    public void shouldMassWithEmptyFiledBeInvalid() {
        assertThat("Incorrect mass time config: none of the fields have are populated: time and singleStartTimestamp",
                MassService.isMassTimeConfigIsValid(new Mass()), is(equalTo(false)));
    }

    @Test
    public void shouldMassWithTimeAndTimestampBeInvalid() {
        Mass massTOCheck = new Mass();
        massTOCheck.setTime("09:00");
        massTOCheck.setSingleStartTimestamp(1L);
        assertThat("Incorrect mass time config: only one of fields have to be populated: time or singleStartTimestamp",
                MassService.isMassTimeConfigIsValid(massTOCheck), is(equalTo(false)));
    }

    @Test
    public void shouldMassWithOnlyTimeBeValid() {
        Mass massTOCheck = new Mass();
        massTOCheck.setTime("09:00");
        assertThat("Incorrect mass time config: only one of fields have to be populated: time or singleStartTimestamp",
                MassService.isMassTimeConfigIsValid(massTOCheck), is(equalTo(true)));
    }

    @Test
    public void shouldMassWithOnlyTimestampBeValid() {
        Mass massTOCheck = new Mass();
        massTOCheck.setSingleStartTimestamp(1l);
        assertThat("Incorrect mass time config: only one of fields have to be populated: time or singleStartTimestamp",
                MassService.isMassTimeConfigIsValid(massTOCheck), is(equalTo(true)));
    }


    @Test
    public void shouldNotPeriodicMassBeInDateRangeWithWrongEndDate() {
        Mass massTOCheck = new Mass();
        massTOCheck.setTime("09:00");
        massTOCheck.setEndDate(LocalDate.parse("10/04/2021", formatter));
        massTOCheck.setDays(new int[]{3});

        assertThat("Incorrect periodic mass date range specified, please correct start & end dates",
                MassService.isScheduleMassDaysInDatePeriod(massTOCheck), is(equalTo(true)));

    }

    @Test
    public void shouldPeriodicMassBeInDateRange() {
        Mass massTOCheck = new Mass();
        massTOCheck.setTime("09:00");
        massTOCheck.setDays(new int[]{3});
        assertThat("Incorrect periodic mass date range specified, please correct start & end dates",
                MassService.isScheduleMassDaysInDatePeriod(massTOCheck), is(equalTo(true)));

    }

    @Test
    public void shouldPeriodicMassBeInDateRangeNotFullDate() {
        Mass massTOCheck = new Mass();
        massTOCheck.setTime("09:00");
        massTOCheck.setStartDate(LocalDate.parse("10/03/2021", formatter));
        massTOCheck.setEndDate(LocalDate.parse("10/04/2021", formatter));
        massTOCheck.setDays(new int[]{3});
        assertThat("Incorrect periodic mass date range specified, please correct start & end dates",
                MassService.isScheduleMassDaysInDatePeriod(massTOCheck), is(equalTo(false)));
    }


}
