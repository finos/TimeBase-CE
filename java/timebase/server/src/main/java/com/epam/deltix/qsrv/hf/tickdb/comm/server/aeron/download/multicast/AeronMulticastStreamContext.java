package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.download.multicast;

import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.vsocket.VSChannel;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Alexei Osipov
 */
public class AeronMulticastStreamContext {
    private final AeronMulticastCursorMetadata cursorMetadata = new AeronMulticastCursorMetadata();
    private final InstrumentMessageSource cursor;
    private final int aeronDataStreamId;
    private final String aeronChannel;
    private final ConcurrentHashMap<VSChannel, Boolean> subscribers = new ConcurrentHashMap<>();
    private volatile boolean stopped = false;

    public AeronMulticastStreamContext(InstrumentMessageSource cursor, int aeronDataStreamId, String aeronChannel) {
        this.cursor = cursor;
        this.aeronDataStreamId = aeronDataStreamId;
        this.aeronChannel = aeronChannel;
    }

    public InstrumentMessageSource getCursor() {
        return cursor;
    }

    public int getAeronDataStreamId() {
        return aeronDataStreamId;
    }

    public String getAeronChannel() {
        return aeronChannel;
    }

    public void addSubscriber(VSChannel vsChannel) {
        Boolean result = subscribers.put(vsChannel, Boolean.TRUE);
        assert result == null;
    }

    public boolean removeSubscriber(VSChannel channel) {
        Boolean value = subscribers.remove(channel);
        assert value == Boolean.TRUE;

        return subscribers.isEmpty();
    }

    public boolean isStopped() {
        return stopped;
    }

    public void markStopped() {
        this.stopped = true;
    }

    public AeronMulticastCursorMetadata getCursorMetadata() {
        return cursorMetadata;
    }
}
