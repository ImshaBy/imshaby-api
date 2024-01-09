package by.imsha.aop.timing;

public class TimingThreadLocal extends ThreadLocal<Timing> {

    @Override
    protected Timing initialValue() {
        return new Timing();
    }
}
