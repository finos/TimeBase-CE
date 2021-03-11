package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.channel.*;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.Util;

import java.io.Flushable;
import java.io.IOException;

/**
 * 
 */
final class TickLoaderImpl extends AbstractTickLoader implements Flushable {

    private MessageChannel<InstrumentMessage> chain;
    private MessageChannel <InstrumentMessage>    downstream;
    private String          user;
    private String          application;

    TickLoaderImpl (TickStreamImpl stream, LoadingOptions opts) {
        super (stream, opts);

        chain = buildChannelChain ();
    }

    protected MessageChannel <InstrumentMessage>      buildChannelChain () {
        MessageChannel <InstrumentMessage>  channel;

        // in case of global sorting make sure that a downstream uses raw encoding
        if (options.globalSorting && !options.raw) {
            final LoadingOptions downChannelOptions = new LoadingOptions();
            downChannelOptions.copy(options);
            downChannelOptions.raw = true;
            downChannelOptions.globalSorting = false;
            downstream = channel = stream.createChannel(null, downChannelOptions);
        } else {
            downstream = channel = stream.createChannel(null, options);
        }

        if (options.globalSorting)
            channel =
                new GlobalSortChannel (
                    Util.fractionOfAvailableMemory (0.2),
                    channel,
                    options,
                    Streams.catTypes (stream)
                );
        return (channel);
    }

    // must be used only in case of global sorting
    public double               getProgress() {
        return ((GlobalSortChannel) chain).getProgress ();
    }

    public synchronized void    send (InstrumentMessage msg) {
        try {
            chain.send (msg);
            register (msg);
        } catch (LoadingError ex) {
            onError (ex);
        } catch (NullPointerException npe) {
            throw new WriterClosedException("Loader " + this + " is closed.", npe);
        } catch (RuntimeException ex) {
            onError (new WriterClosedException(ex));
            throw ex;
        }
    }

    @Override
    public synchronized void    removeUnique(InstrumentMessage msg) {
        if (stream.accumulator != null)
            stream.accumulator.remove(msg);
    }

    public void                 close () {
        Util.close(chain);
        chain = null;

        super.close();
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String getApplication() {
        return application;
    }

    @Override
    public void setApplication(String application) {
        this.application = application;
    }

    @Override
    public String               toString () {
        return super.toString () + " ==> " + stream.getId ();
    }

    @Override
    public void flush()         throws IOException {
        if (downstream instanceof Flushable)
            ((Flushable)downstream).flush();
    }
}
