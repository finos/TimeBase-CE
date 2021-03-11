package com.epam.deltix.qsrv.hf.tickdb.comm.client;


abstract class SubscriptionAction {

    public final long                               serial;
    public final TickCursorClient.ReceiverState     newState;

    protected SubscriptionAction(long serial, TickCursorClient.ReceiverState state) {
        this.serial = serial;
        this.newState = state;
    }

    public abstract void    apply();
}