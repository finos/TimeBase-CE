package com.epam.deltix.util.time;

final class RealtimeClock implements Clock {
    private static final boolean AVAILABLE;
    private static final long RESOLUTION;

    public static final Clock INSTANCE;

    RealtimeClock() {
    }

    public String toString() {
        return "realtime-clock";
    }

    public boolean available() {
        return AVAILABLE;
    }

    public long resolution() {
        return RESOLUTION;
    }

    public long time() {
        return AVAILABLE ? getTime() : System.currentTimeMillis() * RESOLUTION;
    }

    private static native boolean isAvailable();

    private static native long getResolution();

    private static native long getTime();

    static {
        AVAILABLE = Library.LOADED && isAvailable();
        RESOLUTION = AVAILABLE ? getResolution() : 1000000L;

        INSTANCE = (Library.OS.equals("macOsX") ? new GetTimeOfDay("realtime-clock") : new RealtimeClock());
    }
}
