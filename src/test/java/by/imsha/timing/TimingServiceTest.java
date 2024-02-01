package by.imsha.timing;

import by.imsha.aop.timing.TimingService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimingServiceTest {

    @Test
    void testTimingWithTwoPasses() throws InterruptedException {
        TimingService.startTime("test");
        Thread.sleep(100);
        TimingService.stopTime("test");

        TimingService.startTime("test");
        Thread.sleep(100);
        TimingService.stopTime("test");

        String result = TimingService.getResultServerTimingAndRemove();

        assertThat(result).matches("^test=\\d{3}$");
        assertThat(Long.parseLong(result.replaceAll("\\D", ""))).isGreaterThanOrEqualTo(200);
    }

    @Test
    void testTimingThreadLocalRemove() throws InterruptedException {
        TimingService.startTime("test");
        Thread.sleep(100);
        TimingService.stopTime("test");

        String result = TimingService.getResultServerTimingAndRemove();
        assertThat(result).matches("^test=\\d{3}$");

        TimingService.startTime("test2");
        Thread.sleep(100);
        TimingService.stopTime("test2");

        result = TimingService.getResultServerTimingAndRemove();
        assertThat(result).matches("^test2=\\d{3}$");
    }

}