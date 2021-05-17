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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.upload;

import com.epam.deltix.data.stream.MessageDecoder;
import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.UploadHandlerSubChangeListener;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.UserLogger;
import com.epam.deltix.qsrv.hf.tickdb.impl.IdleStrategyProvider;
import com.epam.deltix.qsrv.hf.tickdb.impl.ServerLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingError;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.vsocket.ChannelClosedException;
import com.epam.deltix.util.vsocket.ConnectionAbortedException;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSChannelState;
import io.aeron.ControlledFragmentAssembler;
import io.aeron.Subscription;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.IdleStrategy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.security.Principal;
import java.util.logging.Level;

/**
 * @author Alexei Osipov
 */
public class AeronUploadTask implements Runnable, ControlledFragmentHandler {

    private final Principal user;
    private final Subscription subscription;
    private final String clientId;
    private final TickLoader<RawMessage> loader;
    private final String streamKey;

    private final IdleStrategy consumeIdleStrategy;

    //private final AeronPublicationDSAdapter aeronPublisher;
    private final MessageDecoder<RawMessage> decoder;
    private final VSChannel channel;
    private final DataOutputStream out;


    private AeronUploadLockHolder lockHolder = null;
    private volatile boolean stopped = false;
    private volatile boolean closeChannel = false;


    private final AeronInputBufferAdapter inputStreamAdapter = new AeronInputBufferAdapter();

    private final UploadHandlerSubChangeListener listener;

    public AeronUploadTask(Principal user, Subscription subscription, TickLoader loader, String streamKey, int publicationStreamId, MessageDecoder<RawMessage> decoder, boolean binary, ChannelPerformance channelPerformance, AeronUploadLockHolder lockHolder, VSChannel channel) {
        this.user = user;
        //this.aeronPublisher = AeronPublicationDSAdapter.create(publicationStreamId, aeron, IdleStrategyProvider.getIdleStrategy(channelPerformance));
        this.subscription = subscription;
        this.clientId = channel.getClientId();
        this.loader = loader;
        this.streamKey = streamKey;
        this.decoder = decoder;
        //IdleStrategy publicationIdleStrategy = IdleStrategyProvider.getIdleStrategy(channelPerformance);
        //AeronPublicationDSAdapter aeronPublisherForListener = AeronPublicationDSAdapter.create(publicationStreamId, aeron, publicationIdleStrategy);
        this.out = channel.getDataOutputStream();
        this.listener = new UploadHandlerSubChangeListener(channel, binary, this::closeAll);
        this.consumeIdleStrategy = IdleStrategyProvider.getIdleStrategy(channelPerformance);
        this.lockHolder = lockHolder;
        this.channel = channel;
        this.channel.setAvailabilityListener(new DisconnectListener());
    }

    /**
     * Note: this triggers data send.
     */
    public void installListeners() {
        this.loader.addEventListener(this.listener);
        this.loader.addSubscriptionListener(this.listener);
    }


    @Override
    public void run() {
        ControlledFragmentAssembler fragmentHandler = new ControlledFragmentAssembler(this);
        try {
            while (!stopped) {
                consumeIdleStrategy.idle(subscription.controlledPoll(fragmentHandler, 1000));
            }
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        subscription.close();
        //aeronPublisher.close();
        //this.listener.aeronPublisher.close();
        closeLoader();
        lockHolder.close();

        channel.setAvailabilityListener(null);
        if (closeChannel) {
            channel.close(true);
        }
    }

    /**
     * Marks that all resources should be closed. Use this method if channel might be in dirty state (i.e. IOException occurred).
     */
    private void closeAll() {
        closeChannel = true;
        close();
    }

    /**
     * Marks for graceful shutdown. Channel will not be closed and might be reused.
     */
    private void close() {
        stopped = true;
    }

    private void            closeLoader() {
        loader.removeEventListener (listener);
        loader.removeSubscriptionListener(listener);
        loader.close ();
    }

    @Override
    public Action onFragment(DirectBuffer buffer, int offset, int length, Header header) {
        ServerLock lock = lockHolder.getLock();
        if (lock != null && lock.getClientId() != null) {
            if (!lock.isAcceptable(clientId)) {
                LoadingError error = new StreamLockedException("Loader aborted because of " + lock);
                TickDBServer.LOGGER.log (Level.WARNING, error.getMessage());
                listener.onError(error);
                stopped = true;
                throw error;
            }
        }
        if (inputStreamAdapter.isMultipart()) {
            if (inputStreamAdapter.copyFromDirectForMultipart(buffer, offset, length)) {
                // We got all parts
                processMessageFromAdapter(inputStreamAdapter.isMultipartRemove());
            }
            return Action.CONTINUE;
        }

        DataInputStream dis =  inputStreamAdapter.copyFromDirect(buffer, offset, length);
        try {
            int code = dis.readByte();
            //int     size = length;//MessageSizeCodec.read (ds.getInputStream());

            if (code < 0) {
                TickDBServer.LOGGER.warning (
                        "Unexpected EOS loading into " + streamKey
                );
                stopped = true;
                return Action.BREAK;
            }

            boolean  remove = false;


            switch (code) {
                case TDBProtocol.LOAD_CLOSE:  // Clean disconnect
                {
                    closeLoader();
                    //DataOutputStream out = aeronPublisher.getDataOutputStream();
                    synchronized (out) {
                        out.writeInt(TDBProtocol.LOADRESP_CLOSE_OK);
                        out.flush();
                    }

                    close();
                    return Action.BREAK;
                }
                case TDBProtocol.LOAD_FLUSH: {
                    //DataOutputStream out = aeronPublisher.getDataOutputStream();
                    synchronized (out) {
                        out.writeInt(TDBProtocol.LOADRESP_FLUSH_OK);
                        out.flush();
                    }
                    break;
                }

                case TDBProtocol.LOAD_REMOVE:
                    //size = MessageSizeCodec.read((DataInput) dis);
                    remove = true;
                    // we do not break - read message
                case TDBProtocol.LOAD_MSG: {
                    processMessageFromAdapter(remove);
                    break;
                }

                case TDBProtocol.LOAD_MULTIPART_HEAD_REMOVE:
                    remove = true;
                    // No break
                case TDBProtocol.LOAD_MULTIPART_HEAD: {
                    int dataSize = dis.readInt();
                    inputStreamAdapter.startMultipart(dataSize, remove);
                    break;
                }

                default:
                    throw new IllegalStateException("Unexpected request code: " + code);
            }
            return Action.CONTINUE;
        } catch (ChannelClosedException x) {
            TickDBServer.LOGGER.finest("Client disconnect");
            closeAll ();
        } catch (ConnectionAbortedException e) {
            UserLogger.warn(user, channel.getRemoteAddress(), channel.getRemoteApplication(), " client connection dropped.", e);
            closeAll();
        } catch (EOFException iox) {
            TickDBServer.LOGGER.log(Level.FINEST, "Client disconnect", iox);
            closeAll ();
        } catch (StreamLockedException iox) {
            closeLoader();
            close();
        } catch (Throwable iox) {
            UserLogger.severe(user, channel.getRemoteAddress(), channel.getRemoteApplication(), "Error while loading data.", iox);
            closeAll();
            return Action.ABORT;
        }
        return Action.BREAK;
    }

    private void processMessageFromAdapter(boolean remove) {
        MemoryDataInput inBuffer = inputStreamAdapter.getAsMdi();

        long nanos = TimeCodec.readNanoTime(inBuffer);
        RawMessage msg = decoder.decode(null, inBuffer);

        msg.setNanoTime(nanos);

        if (remove)
            loader.removeUnique(msg);
        else
            loader.send(msg);
    }

    private class DisconnectListener implements Runnable {
        @Override
        public void run() {
            if (channel.getState() != VSChannelState.Connected) {
                stopped = true;
            }
        }
    }
}
