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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.download.multicast;

import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSChannelState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Level;

import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.CURREQ_GET_MULTICAST_CURSOR_METADATA;

/**
 * @author Alexei Osipov
 */
public final class AeronMulticastDownloadControlTask extends QuickExecutor.QuickTask {

    public final Runnable avlnr = AeronMulticastDownloadControlTask.this::submit;

    private volatile boolean stopped = false;

    private final String streamKey;
    private final VSChannel channel;
    private final DXServerAeronContext aeronContext;
    private final AeronMulticastCursorMetadata metadata;
    private boolean subscribed = true;

    public AeronMulticastDownloadControlTask(QuickExecutor exe, String streamKey, VSChannel channel, DXServerAeronContext aeronContext, AeronMulticastCursorMetadata metadata) {
        super(exe);
        this.streamKey = streamKey;
        this.channel = channel;
        this.aeronContext = aeronContext;
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return ("Control Task for " + this);
    }

    public void stop() {
        unschedule();
        stopped = true;
    }

    @Override
    public void run() {
        if (!subscribed) {
            // We already aborted processing
            return;
        }
        if (channel.getState() != VSChannelState.Connected) {
            shutdown();
            return;
        }

        DataInputStream din = channel.getDataInputStream();
        try {
            for (; ; ) {
                if (stopped)
                    break;

                if (din.available() < 2)
                    return;

                processCommand(din);
            }
        } catch (EOFException iox) {
            // valid close
            shutdown();
        } catch (IOException | RuntimeException ex) {
            TickDBServer.LOGGER.log(
                    Level.INFO,
                    "Exception on download control channel",
                    ex
            );
            shutdown();
        }
    }

    private void processCommand(DataInputStream din) throws IOException {
        short commandCode = din.readShort();
        switch (commandCode) {
            case CURREQ_GET_MULTICAST_CURSOR_METADATA:
                sendMetadata();
                break;
            default:
                throw new IllegalArgumentException("Unknown command code" + commandCode);
        }
    }

    private void sendMetadata() throws IOException {
        int entityDataLength = metadata.getEntityDataLength();
        int typeDataLength = metadata.getTypeDataLength();
        DataOutputStream dout = channel.getDataOutputStream();
        dout.writeInt(entityDataLength);
        dout.writeInt(typeDataLength);
        dout.write(metadata.getEntityDataBuffer(), 0, entityDataLength);
        dout.write(metadata.getTypeDataBuffer(), 0, typeDataLength);
        dout.flush();
    }

    private void shutdown() {
        subscribed = false; // We should unsubscribe only once
        aeronContext.unsubscribeFromMulticast(streamKey, channel);
    }
}
