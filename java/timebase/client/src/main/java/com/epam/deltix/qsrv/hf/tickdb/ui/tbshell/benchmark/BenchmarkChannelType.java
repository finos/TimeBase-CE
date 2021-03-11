package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark;

/**
 * @author Alexei Osipov
 */
public enum BenchmarkChannelType {
    DURABLE_STREAM("dstream", "Durable stream"),
    TRANSIENT_STREAM("tstream", "Transient stream"),
    TOPIC("ipctopic", "Topic IPC"),
    UDP_SINGLE_PRODUCER_TOPIC("udpsingletopic", "Topic UDP Single Producer");

    private final String key;
    private final String name;

    BenchmarkChannelType(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public static BenchmarkChannelType getByKey(String key) {
        for (BenchmarkChannelType value : values()) {
            if (value.getKey().equalsIgnoreCase(key)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid type key: " + key);
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }
}
