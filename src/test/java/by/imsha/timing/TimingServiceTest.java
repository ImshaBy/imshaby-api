package by.imsha.timing;

import by.imsha.aop.timing.TimingService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TimingServiceTest {

    @Test
    void testTimingWithTwoPasses() throws InterruptedException {
        TimingService.startTime("test");
        Thread.sleep(100);
        TimingService.stopTime("test");

        TimingService.startTime("test");
        Thread.sleep(100);
        TimingService.stopTime("test");

        String result = TimingService.getResultServerTimingAndRemove();

        assertThat(result).matches("^test=\\d{3,}+;$");
    }

}