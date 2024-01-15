package by.imsha.aop.timing;

import java.util.Map;

public class TimingService {

    public static TimingThreadLocal timingThreadLocal = new TimingThreadLocal();

    public static void startTime(String name) {
        Map<String, Timing> timingMap = timingThreadLocal.get();
        if(timingMap.containsKey(name)) {
            Timing timing = timingMap.get(name);
            timing.setLevel(timing.getLevel() + 1);
        } else {
            timingMap.put(name, new Timing(System.currentTimeMillis(), 1));
        }
    }

    public static void stopTime(String name) {
        Map<String, Timing> timingMap = timingThreadLocal.get();
        Timing timing = timingMap.get(name);
        timing.setLevel(timing.getLevel() - 1);
        if (timing.getLevel() == 0) {
            timing.setTime(System.currentTimeMillis() - timing.getTime());
        }
    }

    public static String getResultServerTiming() {
        Map<String, Timing> timingMap = timingThreadLocal.get();
        StringBuilder result = new StringBuilder();
        timingMap.forEach((s, timing) -> result.append(s).append("=").append(timing.getTime()).append(";"));
        return result.toString();
    }

}
