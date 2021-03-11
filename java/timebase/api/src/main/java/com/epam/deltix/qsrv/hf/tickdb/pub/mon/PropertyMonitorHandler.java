package com.epam.deltix.qsrv.hf.tickdb.pub.mon;

/**
 *
 */
public interface PropertyMonitorHandler {

    public void addPropertyMonitor(PropertyMonitor listener);

    public void removePropertyMonitor(PropertyMonitor listener);
}
