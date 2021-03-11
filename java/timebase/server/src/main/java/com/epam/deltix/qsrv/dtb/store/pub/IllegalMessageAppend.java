package com.epam.deltix.qsrv.dtb.store.pub;

/**
 *
 */
public class IllegalMessageAppend extends IllegalStateException {
    private long lastWrittenNanos;

    public IllegalMessageAppend(long lastWrittenNanos) {
        this.lastWrittenNanos = lastWrittenNanos;
    }

    public long getLastWrittenNanos() {
        return lastWrittenNanos;
    }
}
