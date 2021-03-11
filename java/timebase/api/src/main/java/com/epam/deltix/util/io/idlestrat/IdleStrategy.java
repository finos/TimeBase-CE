package com.epam.deltix.util.io.idlestrat;

/**
 * Different implementation
 */
public interface IdleStrategy {
    /**
     * @param workCount amount of work done in the last cycle. Value "0" means that no work as done and some data form an external source expected.
     */
    void idle(int workCount);

    /**
     * Idle action (sleep, wait, etc).
     */
    void idle();

    /**
     * Reset the internal state (after doing some work).
     */
    void reset();
}
