package by.imsha.aop.timing;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class TimingService {

    private static final ThreadLocal<Map<String, Timing>> timingThreadLocal = ThreadLocal.withInitial(HashMap::new);

    public static void startTime(String name) {
        Map<String, Timing> timingMap = timingThreadLocal.get();
        Timing timing = timingMap.computeIfAbsent(name, n -> new Timing());
        timing.increaseLevel();
    }

    public static void stopTime(String name) {
        Map<String, Timing> timingMap = timingThreadLocal.get();
        Timing timing = timingMap.get(name);
        timing.decreaseLevel();
    }

    public static String getResultServerTimingAndRemove() {
        Map<String, Timing> timingMap = timingThreadLocal.get();
        String result = timingMap.entrySet().stream()
                .sorted((r1, r2) ->
                        r2.getValue().getTime().compareTo(r1.getValue().getTime())
                )
                .map(r -> r.getKey() + "=" + r.getValue().getTime() + ";")
                .collect(Collectors.joining());
        timingThreadLocal.remove();
        return result;
    }

}
