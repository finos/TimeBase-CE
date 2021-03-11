package com.epam.deltix.qsrv.hf.tickdb.pub.mon;

/**
 *
 */
public interface PropertyMonitor {

    public void propertyChanged(String owner, String property, Object newValue);
}
