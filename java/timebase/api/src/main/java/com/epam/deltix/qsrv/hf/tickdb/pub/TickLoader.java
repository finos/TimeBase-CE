package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import com.epam.deltix.timebase.messages.MessageInfo;

import java.io.Flushable;

/**
 *
 */
public interface TickLoader<T extends MessageInfo> extends MessageChannel<T>, Flushable {

    public WritableTickStream   getTargetStream ();

    public void         addEventListener (LoadingErrorListener listener);

    public void         removeEventListener (LoadingErrorListener listener);

    public void         addSubscriptionListener (SubscriptionChangeListener listener);

    public void         removeSubscriptionListener (SubscriptionChangeListener listener);

    public void         removeUnique(T msg);
}
