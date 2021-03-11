package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *
 */
public interface StreamStateNotifier {

    public void addStreamStateListener(StreamStateListener listener);

    public void removeStreamStateListener(StreamStateListener listener);
}
