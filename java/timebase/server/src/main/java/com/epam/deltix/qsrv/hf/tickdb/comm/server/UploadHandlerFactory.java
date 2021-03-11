package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.tickdb.comm.LoadingOptionsCodec;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.UnknownStreamException;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.upload.AeronUploadHandler;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.vsocket.VSChannel;
import io.aeron.Aeron;

import java.io.DataInputStream;
import java.io.IOException;
import java.security.Principal;

import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.TRANSPORT_TYPE_AERON;
import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.TRANSPORT_TYPE_SOCKET;

/**
 * Reads content of loader creation request, decides on client type and creates corresponding uploader (socket-based or Aeron-based).
 *
 * @author Alexei Osipov
 */
public class UploadHandlerFactory {
    public static void start(Principal user, DXTickDB db, VSChannel ds, QuickExecutor exe, int clientVersion, DXServerAeronContext aeronContext, AeronThreadTracker aeronThreadTracker) throws IOException {
        DataInputStream in = ds.getDataInputStream();

        boolean aeronSupported = clientVersion >= TDBProtocol.AERON_SUPPORT_VERSION;
        int requestedTransportType = TRANSPORT_TYPE_SOCKET;
        if (aeronSupported) {
            requestedTransportType = in.read();
        }

        boolean binary = true;
        if (clientVersion > 99)
            binary = in.readBoolean ();

        String key = in.readUTF();

        DXTickStream stream = db.getStream (key);

        if (stream == null)
            throw new UnknownStreamException("Unknown stream: " + key);

        DBLock lock = RequestHandler.readLock(ds);
        stream.verify(lock, LockType.WRITE);

        LoadingOptions options = new LoadingOptions(true);
        LoadingOptionsCodec.read(in, options, clientVersion);

        int selectedTransportType = TRANSPORT_TYPE_SOCKET;
        if (requestedTransportType == TRANSPORT_TYPE_AERON && TDBProtocol.ALLOW_AERON_FOR_LOADER && RequestHandler.isLocal(ds)) {
            // We will not use Aeron for MIN_LATENCY mode because it performs not as good as Sockets.
            // It will provide better latency only in LATENCY_CRITICAL mode.
            if (options.channelPerformance != ChannelPerformance.LOW_LATENCY) {
                selectedTransportType = TRANSPORT_TYPE_AERON;
            }
        }

        if (selectedTransportType == TRANSPORT_TYPE_SOCKET) {
            new UploadHandler(user, ds, exe, clientVersion, binary, options, stream, lock);
        } else if (selectedTransportType == TRANSPORT_TYPE_AERON) {
            Aeron aeron = aeronContext.getAeron();
            int aeronErrorStreamId = aeronContext.getNextStreamId();
            int aeronLoaderDataStreamId = aeronContext.getNextStreamId();
            AeronUploadHandler.start(user, ds, clientVersion, aeron, aeronErrorStreamId, aeronLoaderDataStreamId, aeronThreadTracker, aeronContext.getAeronDir(), binary, options, stream, lock);
        } else {
            throw new IllegalStateException("Unknown transport type code: " + selectedTransportType);
        }
    }

}
