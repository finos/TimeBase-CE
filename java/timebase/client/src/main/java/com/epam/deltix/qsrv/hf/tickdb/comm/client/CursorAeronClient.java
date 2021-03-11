package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.util.lang.Disposable;
import io.aeron.*;
import io.aeron.logbuffer.ControlledFragmentHandler;

/**
 * @author Alexei Osipov
 */
public class CursorAeronClient implements Disposable {
    public static final String CHANNEL = TDBProtocol.AERON_CHANNEL;
    public static final int MAX_MESSAGES_TO_GET = 1000;

    private final Subscription dataSubscription;
    private final ControlledFragmentAssembler fragmentHandler;

    private CursorAeronClient(Subscription dataSubscription, ControlledFragmentHandler delegate) {
        this.dataSubscription = dataSubscription;
        this.fragmentHandler = new ControlledFragmentAssembler(delegate);
    }

    public static CursorAeronClient create(int aeronDataStreamId, ControlledFragmentHandler delegate, Aeron aeron, String aeronChannel) {
        Subscription dataSubscription = aeron.addSubscription(aeronChannel, aeronDataStreamId);
        return new CursorAeronClient(dataSubscription, delegate);
    }

    public boolean pageDataIn() {
        return dataSubscription.controlledPoll(fragmentHandler, MAX_MESSAGES_TO_GET) > 0;
    }

    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {
        dataSubscription.close();
    }

    public Subscription getSubscription() {
        return dataSubscription;
    }
}
