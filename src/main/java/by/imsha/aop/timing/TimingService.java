package by.imsha.aop.timing;

import java.util.HashMap;
import java.util.Map;

public class TimingService {

    private static ThreadLocal<Map<String, Timing>> timingThreadLocal = ThreadLocal.withInitial(HashMap::new);

    public static void startTime(String name) {
        Map<String, Timing> timingMap = timingThreadLocal.get();
        Timing timing = timingMap.computeIfAbsent(name, n -> new Timing(System.currentTimeMillis(), 1));
        timing.increaseLevel();
    }

    public static void stopTime(String name) {
        Map<String, Timing> timingMap = timingThreadLocal.get();
        Timing timing = timingMap.get(name);
        timing.decreaseLevel();
    }

    public static String getResultServerTimingAndRemove() {
        Map<String, Timing> timingMap = timingThreadLocal.get();
        StringBuilder result = new StringBuilder();
        timingMap.forEach((s, timing) -> result.append(s).append("=").append(timing.getTime()).append(";"));
        timingThreadLocal.remove();
        return result.toString();
    }

}
