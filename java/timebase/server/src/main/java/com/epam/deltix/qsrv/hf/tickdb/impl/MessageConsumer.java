package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.dtb.store.pub.TSMessageConsumer;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

public abstract class MessageConsumer<T extends InstrumentMessage> implements TSMessageConsumer {

    protected static final int _REALTIME_MESSAGE_TYPE_INDEX              = -1;
    protected static final int _SUPPRESS_REALTIME_MESSAGE_NOTIFICATION   = -2;
    private final int       realTimeStartedMessageTypeIndex;
    private boolean isRealtimeStarted;

    protected int           currentTypeIndex;
    final RegistryCache     registry;
    T                       message;

    protected MessageConsumer(RegistryCache registry, RecordClassDescriptor[] types, boolean realTimeNotification) {
        this.registry = registry;
        this.realTimeStartedMessageTypeIndex = (realTimeNotification) ? indexOf(types, Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR.getGuid()) : _SUPPRESS_REALTIME_MESSAGE_NOTIFICATION;
    }

    public abstract T                       getMessage();

    public abstract RecordClassDescriptor   getCurrentType();

    public final int                        getCurrentTypeIndex() {
        return currentTypeIndex;
    }

    protected static int indexOf(RecordClassDescriptor [] types, String typeGUID) {
        if (types != null)
            for (int i=0; i < types.length; i++)
                if (typeGUID.equals(types[i].getGuid()))
                    return i;
        return _REALTIME_MESSAGE_TYPE_INDEX;
    }

    @Override
    public final boolean processRealTime(long timestampNanos) {
        if ( ! realTimeAvailable())
            return false;

        if (isRealtimeStarted)
            return false;

        isRealtimeStarted = true;

        currentTypeIndex = realTimeStartedMessageTypeIndex;
        message = makeRealTimeStartMessage(timestampNanos);
        return true;
    }

    protected abstract T makeRealTimeStartMessage(long timestampNanos);

    @Override
    public boolean realTimeAvailable() {
        return realTimeStartedMessageTypeIndex != _SUPPRESS_REALTIME_MESSAGE_NOTIFICATION;
    }

    @Override
    public boolean isRealTime() {
        return isRealtimeStarted;
    }
}