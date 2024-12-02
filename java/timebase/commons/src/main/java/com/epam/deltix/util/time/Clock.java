package com.epam.deltix.util.time;

public interface Clock {
    boolean available();

    long resolution();

    long time();
}

