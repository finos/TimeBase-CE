package com.epam.deltix.qsrv.dtb.store.pub;

/**
 * Allows individual services to initiate emergency timebase shutdown.
 *
 * @author Alexei Osipov
 */
@FunctionalInterface
public interface EmergencyShutdownControl {
    void triggerEmergencyShutdown();
}
