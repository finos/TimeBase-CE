package com.epam.deltix.util.time;

final class GetTimeOfDay implements Clock {
    private static final boolean AVAILABLE;
    private static final long RESOLUTION;
    private final String name;

    GetTimeOfDay(String name) {
        this.name = null != name ? name : "get-time-of-day";
    }

    public String toString() {
        return this.name;
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
    }
}
