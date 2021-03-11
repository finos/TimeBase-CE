package com.epam.deltix.qsrv.hf.tickdb.impl.remotestreams;

import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamPropertiesEnum;

import java.util.EnumSet;

/**
 * @author Alexei Osipov
 */
public interface StreamChangeObserver {
    void streamAdded(String streamKey);
    void streamRemoved(String streamKey);
    void streamRenamed(String oldStreamKey, String newStreamKey);

    void streamUpdated(String streamKey, EnumSet<TickStreamPropertiesEnum> changeSet);
}
