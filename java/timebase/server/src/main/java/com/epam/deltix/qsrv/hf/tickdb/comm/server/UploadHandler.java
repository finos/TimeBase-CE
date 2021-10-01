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
package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.data.stream.MessageDecoder;
import com.epam.deltix.qsrv.hf.tickdb.impl.ServerLock;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickLoaderWrapper;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBObject;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.vsocket.ConnectionAbortedException;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.ChannelClosedException;
import com.epam.deltix.util.vsocket.VSChannelState;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.comm.*;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.memory.MemoryDataInput;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.security.Principal;
import java.util.logging.Level;

class UploadHandler extends QuickExecutor.QuickTask implements LockEventListener {
    private final VSChannel                 ds;        
    private final ByteArrayList             byteBuffer = new ByteArrayList (1024);
    private final MemoryDataInput           inBuffer = new MemoryDataInput ();
    private final DXTickStream              stream;
    private final LoadingOptions            options;
    private final DataOutputStream          out;
    private final DataInputStream           in;
    private final MessageDecoder <InstrumentMessage> decoder;
    private final TickLoader                loader;

    private final Principal                 user;
    private final boolean                   binary; // use binary serialization
    private volatile ServerLock             lock;

    private final UploadHandlerSubChangeListener listener;
    private final Runnable              avlnr =
        new Runnable () {
            @Override
            public void    run () {
                //System.out.println ("UPLOAD WAKES UP");
                submit ();
            }
        };

    UploadHandler(Principal user, VSChannel ds, QuickExecutor exe, int clientVersion, boolean binary,
                  @Nonnull LoadingOptions options,
                  @Nonnull DXTickStream stream,
                  @Nullable DBLock lock)
            throws IOException
    {
        super (exe);

        this.user = user;
        this.ds = ds;

        this.out = ds.getDataOutputStream ();
        this.in = ds.getDataInputStream ();

        this.binary = binary;
        this.options = options;
        this.stream = stream;

        // we should abort writer when 'write' lock added.
        if (lock == null && this.stream instanceof LockHandler)
            ((LockHandler) this.stream).addEventListener(this);

        RecordClassSet      md = RequestHandler.getMetaData (this.stream);

        decoder = new SimpleRawDecoder(md.getTopTypes ());
        loader = this.stream.createLoader (this.options);

        ds.setAvailabilityListener (avlnr);
        out.writeInt (TDBProtocol.RESP_OK);
        TransportNegotiation.writeSelectedTransport(clientVersion, out, TDBProtocol.TRANSPORT_TYPE_SOCKET);
        out.flush();

        // subscriptions events will be fired on adding listener
        this.listener = new UploadHandlerSubChangeListener(ds, this.binary, this::closeAll);
        loader.addEventListener (listener);
        loader.addSubscriptionListener(listener);

        setLoaderMonitorInfo(loader);
    }

    private void        setLoaderMonitorInfo(TickLoader loader) {
        TickLoader tbLoader = loader;
        if (tbLoader instanceof TickLoaderWrapper)
            tbLoader = ((TickLoaderWrapper) tbLoader).getNestedInstance();

        if (tbLoader instanceof TBObject) {
            TBObject tbObject = (TBObject) tbLoader;
            tbObject.setUser(user != null ? user.getName() : null);
            tbObject.setApplication(ds.getRemoteApplication());
        }
    }

    @Override
    public String       toString () {
        return ("UploadHandler for " + loader);
    }

    @Override
    public void         run () {
        try {
            loop: for (;;) {
                if (ds.getState() == VSChannelState.Closed)
                    break;

                if (in.available () == 0)
                    return;

                if (lock != null && lock.getClientId() != null) {

                    if (!lock.isAcceptable(ds.getClientId())) {
                        RuntimeException error = new StreamLockedException("Loader aborted because of " + lock);
                        TickDBServer.LOGGER.log (Level.WARNING, error.getMessage());
                        listener.onError((LoadingError)error);
                        throw error;
                    }
                }

                int     size = MessageSizeCodec.read (ds.getInputStream());

                if (size < 0) {
                    TickDBServer.LOGGER.warning (
                        "Unexpected EOS loading into " + stream.getKey ()
                    );
                    closeAll ();
                    return;
                }

                boolean  remove = false;

                switch (size) {
                    case TDBProtocol.LOAD_CLOSE:  // Clean disconnect
                        closeLoader();

                        synchronized (out) {
                            out.writeInt (TDBProtocol.LOADRESP_CLOSE_OK);
                            out.flush ();
                        }

                        ds.close();
                        close();

                        break loop;

                    case TDBProtocol.LOAD_FLUSH:
                        loader.flush();

                        synchronized (out) {
                            out.writeInt(TDBProtocol.LOADRESP_FLUSH_OK);
                            out.flush();
                        }
                        break loop;

                    case TDBProtocol.LOAD_REMOVE:
                        size = MessageSizeCodec.read (ds.getInputStream());
                        remove = true;
                        // we do not break - read message
                    default: {
                        byteBuffer.setSizeUnsafe (size);

                        ds.getDataInputStream().readFully (byteBuffer.getInternalBuffer (), 0, size);

                        inBuffer.setBytes (byteBuffer.getInternalBuffer(), 0, byteBuffer.size());

                        long                nanos = TimeCodec.readNanoTime(inBuffer);
                        RawMessage          msg = (RawMessage) decoder.decode (null, inBuffer);

                        msg.setNanoTime(nanos);

                        if (remove)
                            loader.removeUnique(msg);
                        else
                            loader.send (msg);
                        
                        break;
                    }
                }
            }
        } catch (ChannelClosedException x) {
            TickDBServer.LOGGER.finest("Client disconnect");
            closeAll ();
        } catch (ConnectionAbortedException e) {
            UserLogger.warn(user, ds.getRemoteAddress(), ds.getRemoteApplication(), " client connection dropped.", e);
            //TickDBServer.LOGGER.log (Level.WARNING, "Client unexpectedly drop connection");
            closeAll();
        } catch (EOFException iox) {
            TickDBServer.LOGGER.log(Level.FINEST, "Client disconnect", iox);
            closeAll ();
        } catch (StreamLockedException iox) {
            //TickDBServer.LOGGER.log (Level.SEVERE, "Stream is locked.", iox);
            closeLoader();
            close();
        } catch (Throwable iox) {
            UserLogger.severe(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "Error while loading data.", iox);
            //TickDBServer.LOGGER.log (Level.SEVERE, "IOException on upload:", iox);
            closeAll();
        }
    }

    private void            closeLoader() {
        loader.removeEventListener (listener);
        loader.removeSubscriptionListener(listener);
        loader.close ();
    }

    private void            close () {
        unschedule ();

        ((LockHandler)stream).removeEventListener(this);
    }

    private void            closeAll () {

        unschedule ();

        closeLoader();

        ds.setAvailabilityListener(null);
        ds.close (true);

        ((LockHandler)stream).removeEventListener(this);
    }

    @Override
    public void         lockAdded(DBLock lock) {
        if (this.lock == null && lock instanceof ServerLock)
            this.lock = (ServerLock) lock;
    }

    @Override
    public void         lockRemoved(DBLock lock) {
        
    }
}