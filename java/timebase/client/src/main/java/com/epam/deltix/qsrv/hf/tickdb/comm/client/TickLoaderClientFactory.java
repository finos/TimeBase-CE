package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.tickdb.comm.LoadingOptionsCodec;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.vsocket.VSChannel;
import io.aeron.Aeron;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.TRANSPORT_TYPE_AERON;
import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.TRANSPORT_TYPE_SOCKET;

/**
 * @author Alexei Osipov
 */
public class TickLoaderClientFactory {
    public static TickLoader create(
            TickStreamClient stream,
            LoadingOptions options,
            DXClientAeronContext aeronContext
    ) {
        if (options == null) {
            options = new LoadingOptions();
        }

        DXRemoteDB conn = (DXRemoteDB) stream.getDB();

        boolean ok = false;
        VSChannel ds = null;
        try {
            ds = conn.connect(ChannelType.Output, true, false, options.compression, options.channelBufferSize);

            DataOutputStream out = ds.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_OPEN_LOADER);

            boolean aeronSupported = conn.getServerProtocolVersion() >= TDBProtocol.AERON_SUPPORT_VERSION;
            if (aeronSupported) {
                byte preferredTransport = options.channelPerformance == ChannelPerformance.HIGH_THROUGHPUT ||
                        options.channelPerformance == ChannelPerformance.LATENCY_CRITICAL
                        ? TRANSPORT_TYPE_AERON : TRANSPORT_TYPE_SOCKET;
                out.write(preferredTransport);
            }

            out.writeBoolean(true); // binary serialization
            out.writeUTF(stream.getKey());
            stream.writeLock(out);
            LoadingOptionsCodec.write(out, options, ((DXRemoteDB) stream.getDB()).getServerProtocolVersion());
            out.flush();

            stream.checkResponse(ds);

            DataInputStream in = ds.getDataInputStream();
            int transportType = TRANSPORT_TYPE_SOCKET;
            if (aeronSupported) {
                transportType = in.read();
            }

            TickLoader result;
            if (transportType == TRANSPORT_TYPE_SOCKET) {
                result = new TickLoaderClient(stream, options, ds);
            } else if (transportType == TRANSPORT_TYPE_AERON) {
                String aeronDir = in.readUTF();
                Aeron aeron = aeronContext.getServerSharedAeronInstance(aeronDir);
                int aeronServerMessageStreamId = in.readInt(); // Direction: from server to client
                int aeronLoaderDataStreamId = in.readInt(); // Direction: from client to server
                result = new TickLoaderClientAeron(stream, options, ds, aeron, aeronServerMessageStreamId, aeronLoaderDataStreamId);
            } else {
                throw new IllegalStateException("Unknown transport type code: " + transportType);
            }

            ok = true;
            return result;
        } catch (IOException x) {
            throw new com.epam.deltix.util.io.UncheckedIOException(x);
        } finally {
            if (!ok) {
                Util.close(ds);
            }
        }
    }
}
