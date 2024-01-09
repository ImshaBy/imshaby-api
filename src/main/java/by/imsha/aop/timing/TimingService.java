package by.imsha.aop.timing;

public class TimingService {

    public static TimingThreadLocal timingThreadLocal = new TimingThreadLocal();

    public static void startTime() {
        timingThreadLocal.get().setAppTime(System.currentTimeMillis());
    }

    public static void startControllerTime() {
        timingThreadLocal.get().setControllerTime(System.currentTimeMillis());
    }

    public static void startServiceTime() {
        Timing timing = timingThreadLocal.get();
        timing.setServiceTime(System.currentTimeMillis());
        timing.setServiceLevel(timing.getServiceLevel() + 1);
    }

    public static void startRepositoryTime() {
        Timing timing = timingThreadLocal.get();
        timing.setRepositoryTime(System.currentTimeMillis());
        timing.setRepositoryLevel(timing.getRepositoryLevel() + 1);
    }

    public static void stopTime() {
        Timing timing = timingThreadLocal.get();

        if (timing.getServiceTime() != null) {
            if (timing.getRepositoryTime() != null) {
                timing.setControllerTime(timing.getControllerTime() - timing.getServiceTime());
                timing.setServiceTime(timing.getServiceTime() - timing.getRepositoryTime());
            } else {
                timing.setControllerTime(timing.getControllerTime() - timing.getServiceTime());
            }
        } else if (timing.getRepositoryTime() != null) {
            timing.setServiceTime(timing.getControllerTime() - timing.getRepositoryTime());
        }

        timing.setAppTime(System.currentTimeMillis() - timing.getAppTime());
    }

    public static void stopControllerTime() {
        timingThreadLocal.get().setControllerTime(System.currentTimeMillis() - timingThreadLocal.get().getControllerTime());
    }

    public static void stopServiceTime() {
        Timing timing = timingThreadLocal.get();
        timing.setServiceLevel(timing.getServiceLevel() - 1);
        if (timing.getServiceLevel() == 0) {
            timing.setServiceTime(System.currentTimeMillis() - timing.getServiceTime());
        }
    }

    public static void stopRepositoryTime() {
        Timing timing = timingThreadLocal.get();
        timing.setRepositoryLevel(timing.getRepositoryLevel() - 1);
        if (timing.getServiceLevel() == 0) {
            timing.setRepositoryTime(System.currentTimeMillis() - timing.getRepositoryTime());
        }
    }

    public static String getResultServerTiming() {
        Timing timing = timingThreadLocal.get();
        return String.format("app=%s;controller=%s;service=%s;repository=%s", timing.getAppTime(), timing.getControllerTime(), timing.getServiceTime(), timing.getRepositoryTime());
    }

}
