package by.imsha.aop.timing;

import java.util.HashMap;
import java.util.Map;

public class TimingThreadLocal extends ThreadLocal<Map<String, Timing>> {

    @Override
    protected Map<String, Timing> initialValue() {
        return new HashMap<>();
    }
}
