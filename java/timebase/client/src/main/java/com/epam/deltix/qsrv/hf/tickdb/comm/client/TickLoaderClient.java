/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.data.stream.*;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.MessageInfo;
import com.epam.deltix.util.concurrent.Signal;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.*;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.ChannelClosedException;
import com.epam.deltix.util.vsocket.VSChannelState;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.ArrayList;
import java.net.SocketException;

import net.jcip.annotations.GuardedBy;

/**
 *
 */
class TickLoaderClient implements TickLoader<MessageInfo>, LoadingErrorListener, Flushable {

    private enum State {
        Added, Removed, None
    }

    private class ServerListenerTask extends QuickExecutor.QuickTask {
        private final Signal closed = new Signal();
        private final Signal flushed = new Signal();

        // subscription state 
        private ArrayList<IdentityKey>   ids;
        private ArrayList<String>               types;

        private State                           allEntities = State.None;
        private final HashSet<IdentityKey>      subscribedEntities = new HashSet<IdentityKey>(100);

        private State                           allTypes = State.None;
        private final HashSet<String>           subscribedTypes = new HashSet<String>(100);

        private final DataInputStream     in;

        //public deltix.util.io.UncheckedIOException exception;

        final Runnable              avlnr =
            new Runnable () {
                @Override
                public void                 run () {
                    ServerListenerTask.this.submit ();
                }
            };

        public ServerListenerTask(QuickExecutor executor) {
            super (executor);

            this.in = ds.getDataInputStream();
            ds.setAvailabilityListener(avlnr);
        }

        public void waitForClose(){
            try {
                closed.await();
            } catch (InterruptedException e) {
                TickDBClient.LOGGER.info("Waiting interrupted %s").with(e);
            }
        }

        private void notifyClose() {
            closed.set();
        }

        private void waitForFlush() {
            try {
                flushed.await();
                flushed.verify();
            } catch (InterruptedException e) {
                TickDBClient.LOGGER.info("Flush waiting interrupted %s").with(e);
            }
        }

        private void notifyFlush() {
            flushed.set();
        }

        private void notifyFlush(Throwable t) {
            flushed.set(t);
        }

        void fireEvents(SubscriptionChangeListener listener) {
            if (allEntities != State.None) {
                if (allEntities == State.Added)
                    listener.allEntitiesAdded();
                else
                    listener.allEntitiesRemoved();
            } else {
                // subscribedEntities may change
                Collection<IdentityKey> list =
                    Arrays.asList(subscribedEntities.toArray(new IdentityKey[subscribedEntities.size()]));
                listener.entitiesAdded(list);
            }

            if (allTypes != State.None) {
                if (allTypes == State.Added)
                    listener.allTypesAdded();
                else
                    listener.allTypesRemoved();
            } else {
                // subscribedTypes may change
                Collection<String> list = Arrays.asList(subscribedTypes.toArray(new String[subscribedTypes.size()]));
                listener.typesAdded(list);
            }
        }

        @Override
        public void         run () {

            try {
                for (;;) {

                    if (in.available () < 4)
                        break;

                    processCommand ();
                }
            } catch (EOFException | ChannelClosedException iox) {
                // valid close
                processClose();
            } catch (Throwable iox) {
                if (iox instanceof SocketException)
                    onDisconnected();

                sendError(new LoadingError (iox));
                processClose();
            }
        }

        private void processCommand() throws IOException {
            int         cmd = in.readInt();

            //System.out.println("process command: " + cmd);

            switch (cmd) {
                case TDBProtocol.LOADRESP_ERROR:
                    processLoadingError();
                    break;

                case TDBProtocol.LOADRESP_CLOSE_OK:
                    processClose();
                    break;

                case TDBProtocol.LOADRESP_FLUSH_OK:
                    notifyFlush();
                    break;

                case TDBProtocol.RESP_ERROR: {  // TODO: move this to initialization?
                    try {
                        Throwable throwable = TDBProtocol.readError(ds.getDataInputStream());
                        throw new RuntimeException(throwable);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException("Cannot deserialize exception", e);
                    }
                }

                case TDBProtocol.LOADRESP_ENTITIES_CHANGE:
                    processEntitiesChange();
                    break;

                case TDBProtocol.LOADRESP_TYPES_CHANGE:
                    processTypesChange();
                    break;
            }
        }

        private void processLoadingError() {
            try {
                LoadingError error = (LoadingError) TDBProtocol.readBinary(in);
                sendError(error);
            } catch (IOException | ClassNotFoundException e) {
                sendError(new LoadingError (e));
            }
        }

        private void processClose() {
            // closing socket, clean disconnect
            ds.setAvailabilityListener(null);
            ds.close();

            serverListener.unschedule();

            notifyClose();
            notifyFlush(new ChannelClosedException());
        }

        private void processTypesChange() throws IOException {
            if (types == null)
                types = new ArrayList<String>(100);
            else
                types.clear();

            boolean allTypes = in.readBoolean();
            boolean added = in.readBoolean();
            if (!allTypes) {
                this.allTypes = State.None;

                int size = in.readInt();
                for (int i = 0; i < size; i++)
                    types.add(in.readUTF());

                if (added)
                    subscribedTypes.addAll(types);
                else
                    subscribedTypes.removeAll(types);

                for (int i = 0; i < snListenersSnapshot.length; i++) {
                    if (added)
                        snListenersSnapshot[i].typesAdded(types);
                    else
                        snListenersSnapshot[i].typesRemoved(types);
                }
            } else {
                this.allTypes = added ? State.Added : State.Removed;
                subscribedTypes.clear();

                for (int i = 0; i < snListenersSnapshot.length; i++) {
                    if (added)
                        snListenersSnapshot[i].allTypesAdded();
                    else
                        snListenersSnapshot[i].allTypesRemoved();
                }
            }
        }

        private void processEntitiesChange() throws IOException {
            if (ids == null)
                ids = new ArrayList<IdentityKey>(100);
            else
                ids.clear();

            boolean allEntities = in.readBoolean();
            boolean added = in.readBoolean();
            if (!allEntities) {
                this.allEntities = State.None;

                int size = in.readInt();
                for (int i = 0; i < size; i++)
                    ids.add(TDBProtocol.readIdentityKey(in));

                if (added)
                    subscribedEntities.addAll(ids);
                else
                    subscribedEntities.removeAll(ids);

                for (int i = 0; i < snListenersSnapshot.length; i++) {
                    if (added)
                        snListenersSnapshot[i].entitiesAdded(ids);
                    else
                        snListenersSnapshot[i].entitiesRemoved(ids);
                }
            } else {
                this.allEntities = added ? State.Added : State.Removed;
                this.subscribedEntities.clear();

                for (int i = 0; i < snListenersSnapshot.length; i++) {
                    if (added)
                        snListenersSnapshot[i].allEntitiesAdded();
                    else
                        snListenersSnapshot[i].allEntitiesRemoved();
                }
            }
        }
    }

    private VSChannel                       ds;
    private final DataOutputStream          out;
    private final MemoryDataOutput          outBuffer = new MemoryDataOutput (256);
    private final MessageEncoder            encoder;
    @GuardedBy ("this")
    private boolean                         disabled = false; //TODO: refactor

    private volatile boolean                closed = false;
    private LoadingError                    error;
    private LoadingOptions                  options;            
    
    private ServerListenerTask              serverListener;
    private final DXRemoteDB                conn;
    private final TickStreamClient          stream;

    private final ArrayList<LoadingErrorListener> listeners =
        new ArrayList<LoadingErrorListener>();

    private final ArrayList<SubscriptionChangeListener> subscriptionListeners =
        new ArrayList<SubscriptionChangeListener>();

    private volatile SubscriptionChangeListener [] snListenersSnapshot = { };

    @SuppressWarnings("unchecked")
    public TickLoaderClient (
            TickStreamClient            stream,
            LoadingOptions              options,
            VSChannel                   channel
    )
    {
        this.options = options;
        this.stream = stream;
        this.conn = (DXRemoteDB) stream.getDB();
        this.ds = channel;
        this.out = channel.getDataOutputStream ();

        boolean                     ok = false;

        try {
            QuickExecutor quickExecutor = conn.getQuickExecutor();

            // start server listener after waiting for response,
            // because we get subscription events immediately
            serverListener = new ServerListenerTask(quickExecutor);
            serverListener.submit();

            RecordClassSet          md = stream.getMetaData();
            CodecFactory factory = stream.conn.getCodecFactory(options.channelQOS);

            encoder = options.raw ? new SimpleRawEncoder(md.getTopTypes ()) : new SimpleBoundEncoder(factory, options.getTypeLoader(), md.getTopTypes());

//                    StreamConfigurationHelper.createRawEncoder (null, null, md.getTopTypes ()) :
//                    StreamConfigurationHelper.createBoundEncoder (factory, null, null, options.getTypeLoader(), md.getTopTypes ());

            ds.setNoDelay(options.channelPerformance.isLowLatency());
            ok = true;


        } finally {
            if (!ok)
                Util.close (ds);
        }
    }

    public WritableTickStream       getTargetStream () {
        return (stream);
    }

    @SuppressWarnings ("unchecked")
    @Override
    public synchronized void        send (MessageInfo msg) {
        validate(true);
        
        outBuffer.reset ();
        TimeCodec.writeTime (msg, outBuffer);

        if (!encoder.encode (msg, outBuffer))
            return;

        int size = outBuffer.getSize ();
        assert size > TDBProtocol.LOAD_REMOVE: "Size too small: " + size;

        try {
            MessageSizeCodec.write (size, out);
            out.write (outBuffer.getBuffer (), 0, size);
        } catch (ChannelClosedException e) {
            validate(true);
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } catch (IOException iox) {
            if (iox instanceof SocketException)
                onDisconnected();

            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }
    }

    @SuppressWarnings ("unchecked")
    @Override
    public synchronized void        removeUnique(MessageInfo msg) {

        validate(true);

        outBuffer.reset ();
        TimeCodec.writeTime (msg, outBuffer);
        encoder.encode (msg, outBuffer);

        int size = outBuffer.getSize ();
        assert size > TDBProtocol.LOAD_REMOVE: "it is reserved value " + size;

        try {
            out.writeByte(TDBProtocol.LOAD_REMOVE);
            MessageSizeCodec.write (size, out);
            out.write (outBuffer.getBuffer (), 0, size);
        } catch (ChannelClosedException e) {
            validate(true);
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        } catch (IOException iox) {
            if (iox instanceof SocketException)
                onDisconnected();

            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }
    }

    @Override
    public void                     addEventListener(LoadingErrorListener listener) {
        synchronized(listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void                     removeEventListener(LoadingErrorListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }

    private LoadingErrorListener [] listeners () {
        synchronized(listeners) {
            return (listeners.toArray (new LoadingErrorListener [listeners.size ()]));
        }
    }

    private void                    updateSubscriptionSnapshot () {
        int     n = subscriptionListeners.size ();
        snListenersSnapshot = subscriptionListeners.toArray (
                new SubscriptionChangeListener[n]);
    }

    @Override
    public void                     addSubscriptionListener(SubscriptionChangeListener listener) {
        synchronized(subscriptionListeners) {
            subscriptionListeners.add(listener);
            updateSubscriptionSnapshot();
        }

        if (serverListener.ids != null) // we have events
            serverListener.fireEvents(listener);
    }

    @Override
    public void                     removeSubscriptionListener(SubscriptionChangeListener listener) {
        synchronized(subscriptionListeners) {
            subscriptionListeners.remove(listener);
            updateSubscriptionSnapshot();
        }
    }

    private void                    sendError (LoadingError e) {
        if (e instanceof StreamLockedException)
            error = e;
        else if (e instanceof WriterAbortedException)
            error = e;
        else if (e instanceof WriterClosedException)
            error = e;

        validate(false);

        for (LoadingErrorListener listener : listeners ())
            listener.onError(e);
    }

    @Override
    public void                 onError (LoadingError e) {
        LoadingOptions.ErrorAction action;

        synchronized (this) {
            action = options.getErrorAction (e.getClass());
        }

        if (action == LoadingOptions.ErrorAction.NotifyAndAbort)
            throw e;

        if (action == LoadingOptions.ErrorAction.NotifyAndContinue) {
            for (LoadingErrorListener listener : listeners ())
                listener.onError (e);
        }
    }

    /**
     * Closes tick loader and waits for acknowledgement.
     */
    public synchronized void        close () {
        if (closed)
            return;

        if (ds.getState() == VSChannelState.Connected) {
            try {
                out.writeByte (TDBProtocol.LOAD_CLOSE);
                out.flush ();

                serverListener.waitForClose ();
            } catch (IOException iox) {
                TickDBClient.LOGGER.warn("Error disconnecting from server (ignore) - %s").with(iox);
            }
        }

        closed = true;
        ds.close(); // we should close channel in any case
    }

    @Override
    public synchronized void        flush() throws IOException {
        validate(true);

        out.writeByte(TDBProtocol.LOAD_FLUSH);
        out.flush();

        serverListener.waitForFlush();
    }

    private void        onDisconnected() {
        if (!closed)
            disabled = true;
    }

    private void        validate(boolean explode) {
        if (error != null) {

            ds.close(true);
            closed = true;
            
            if (explode)
                throw error;
        }
        
        if (disabled)
            throw new IllegalStateException("Loader is disabled upon a disconnection event.");
    }

    @Override
    public String toString () {
        return super.toString() + " (" + stream.getKey() + ')';
    }
}