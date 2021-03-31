package by.imsha.service;

import by.imsha.domain.Mass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MassServiceTest {

    private static MassService massService;

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
    public void should() {


    }


}
