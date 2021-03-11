package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.pub.ChannelCompression;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.vsocket.VSChannel;
import io.aeron.Aeron;
import org.apache.commons.lang3.StringUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;

import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.REQ_CREATE_MULTICAST_CURSOR;
import static com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol.TRANSPORT_TYPE_AERON;

/**
 * @author Alexei Osipov
 */
public class TickCursorClientFactoryMulticast {

    public static MessageSource<InstrumentMessage> create(
            DXRemoteDB db,
            TickStream stream,
            boolean raw, DXClientAeronContext aeronContext) {

        boolean     ok = false;
        VSChannel tmpds = null;

        try {
            tmpds = db.connect (ChannelType.Input, false, false, ChannelCompression.AUTO, 0 /*default*/);

            DataOutputStream out = tmpds.getDataOutputStream ();
            out.writeInt (REQ_CREATE_MULTICAST_CURSOR);

            out.writeBoolean(true); // binary serialization

            TickCursorClient.writeStreamKeys(out, new TickStream[]{stream});

            out.flush ();

            final DataInputStream in = tmpds.getDataInputStream();
            boolean success = in.readBoolean ();

            if (TickCursorClient.DEBUG_COMM) {
                TickDBClient.LOGGER.info (TickCursorClientFactoryMulticast.class + ": CREATE {" +
                        stream + "}");
            }

            if (!success) {
                TickCursorClient.processError(in);
                throw new AssertionError("Unreachable");
            } else {
                int transportType = in.read();
                if (transportType != TRANSPORT_TYPE_AERON) {
                    throw new AssertionError();
                }

                String aeronDir = in.readUTF();
                String aeronChannel = in.readUTF();
                int aeronDataStreamId = in.readInt();

                Aeron aeron;
                if (StringUtils.isNotBlank(aeronDir)) {
                    aeron = aeronContext.getServerSharedAeronInstance(aeronDir);
                } else {
                    aeron = aeronContext.getStandaloneAeronInstance();
                }

                TickCursorClientAeronMulticast result = new TickCursorClientAeronMulticast(db, tmpds, raw, stream, aeronContext.getSubscriptionChecker(), aeronDataStreamId, aeron, aeronChannel);
                ok = true;
                return result;
            }
        } catch (IOException x) {
            if (x instanceof SocketException) {
                if (TickCursorClient.isChannelClosed(tmpds)) {
                    throw new IllegalStateException (
                            "Cursor is closed either by a client or upon a disconnection event."
                    );
                }
            }

            if (x instanceof InterruptedIOException) {
                throw new UncheckedInterruptedException(x);
            }

            throw new com.epam.deltix.util.io.UncheckedIOException(x);
        } finally {
            if (!ok) {
                Util.close(tmpds);
            }
        }
    }
}
