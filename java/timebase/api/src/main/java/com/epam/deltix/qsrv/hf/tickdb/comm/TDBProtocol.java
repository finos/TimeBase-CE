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
package com.epam.deltix.qsrv.hf.tickdb.comm;

import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.TimeInterval;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.pub.md.UHFJAXBContext;
import com.epam.deltix.qsrv.hf.pub.util.SerializationUtils;
import com.epam.deltix.qsrv.hf.tickdb.impl.FriendlyStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.BackgroundProcessInfo;
import com.epam.deltix.qsrv.hf.tickdb.pub.BufferOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SerializableTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationContext;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationTask;
import com.epam.deltix.util.collections.generated.IntegerHashSet;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.progress.ExecutionStatus;
import com.epam.deltix.util.time.Periodicity;
import com.epam.deltix.util.vsocket.VSChannel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.aeron.CommonContext;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.List;


/**
 *
 */
public abstract class TDBProtocol extends SerializationUtils {
    public static final boolean USE_MAGIC = false;
    public static final boolean SEND_SEQUENCE = false;

    public static final boolean ALLOW_AERON_FOR_CURSOR = false;
    public static final boolean ALLOW_AERON_FOR_LOADER = false;

    @SuppressWarnings("ConstantConditions")
    public static final boolean NEEDS_AERON_DRIVER = ALLOW_AERON_FOR_CURSOR || ALLOW_AERON_FOR_LOADER;

    public static final boolean USE_TIME_CODEC_FOR_AERON = false;
    public static final String AERON_CHANNEL = CommonContext.IPC_CHANNEL;
    //public static final String AERON_UDP_CHANNEL = "aeron:udp?endpoint=localhost:40123";fVEr

    public static final String  PROTOCOL_ID = "dxtick";
    public static final String  SSL_PROTOCOL_ID = "dstick";
    public static final String  PROTOCOL_PREFIX = PROTOCOL_ID + "://";
    public static final String  SSL_PROTOCOL_PREFIX = SSL_PROTOCOL_ID + "://";
//    public static final String  CHOICE_PROTOCOL_ID = "dxchoice";
//    public static final String  CHOICE_PROTOCOL_PREFIX = CHOICE_PROTOCOL_ID + "://";

    public static final int     DEFAULT_PORT = 8011;
    public static final int     DEFAULT_SSL_PORT = TDBProtocol.getSSLPort(DEFAULT_PORT);

    public static final String  DEFAULT_URL = PROTOCOL_PREFIX + "localhost:" + DEFAULT_PORT;
    public static final String  SSL_DEFAULT_URL = SSL_PROTOCOL_ID + "localhost:" + DEFAULT_PORT;

    /**
     *  This component's version, whether server or client.
     */
    public static final int     VERSION = 132;
    
    /**
     *  Server will refuse to talk to a client unless the client's version is   at least
     *  this number.
     */
    public static final int     MIN_CLIENT_VERSION = 130;
    
    /**
     *  Client will refuse to talk to a server unless the server's version is at least 
     *  this number.
     */
    public static final int     MIN_SERVER_VERSION = VERSION;

    public static final int     AERON_SUPPORT_VERSION = 106;
    
    public static final int     NO_CHANGES = -1;

    public static final int     READ_ONLY = 1;
    public static final int     READ_WRITE = 0;
    public static final int     INCOMPATIBLE_CLIENT_PROTOCOL_VERSION = 100;
    
    public static final int     REQ_CONNECT =           1;
    public static final int     REQ_LIST_STREAMS =      2;
    public static final int     REQ_GET_MD_VERSION =    3;
    public static final int     REQ_GET_METADATA =      4;
    public static final int     REQ_SET_METADATA =      5;

    public static final int     REQ_TRIM_TO_SIZE =      100;
    public static final int     REQ_GET_STREAM =        101;
    public static final int     REQ_GET_STREAM_NAME =   103;
    public static final int     REQ_GET_STREAM_DESCR =  104;
    public static final int     REQ_GET_STREAM_TNF =    105;
    public static final int     REQ_GET_TIME_RANGE =    106;
    public static final int     REQ_LIST_ENTITIES =     107;
    public static final int     REQ_SET_STREAM_TYPE =   108;
    public static final int     REQ_GET_SIZE =          109;
    public static final int     REQ_GET_STREAM_TYPE =   110;
    public static final int     REQ_GET_STREAM_TYPE_VERSION = 111;
    public static final int     REQ_LIST_TIME_RANGE =   112;
    
    public static final int     REQ_PURGE_STREAM =      114;
    public static final int     REQ_GET_BG_PROCESS =    115;
    public static final int     REQ_ABORT_BG_PROCESS =  116;
    public static final int     REQ_CLEAR_DATA =        117;
    public static final int     REQ_SET_STREAM_NAME =   118;
    public static final int     REQ_SET_STREAM_DESCR =  119;
    //public static final int     REQ_GET_LIVE_FILTER =   120;
    public static final int     REQ_GET_STREAM_PERIOD = 121;
    public static final int     REQ_SET_STREAM_PERIOD = 122;
    public static final int     REQ_TRUNCATE_DATA =     123;
    public static final int     REQ_GET_SERVER_TIME =   124;
    public static final int     REQ_GET_STREAM_OPTIONS =125;
    public static final int     REQ_LOCK_STREAM =       126;
    public static final int     REQ_TRY_LOCK_STREAM =   127;
    public static final int     REQ_UNLOCK_STREAM =     128;
    public static final int     REQ_GET_LOCK_STATE =    129;
    public static final int     REQ_GET_STREAM_HA =     130;
    public static final int     REQ_SET_STREAM_HA =     131;
    public static final int     REQ_ENABLE_VERSIONING = 132;
    public static final int     REQ_GET_DATA_VERSION  = 133;
    public static final int     REQ_GET_REPLICA_VERSION  = 134;
    public static final int     REQ_SET_REPLICA_VERSION  = 135;
    public static final int     REQ_GET_INSTR_COMPOSITION  = 136;
    public static final int     REQ_SET_STREAM_OWNER =  137;
    public static final int     REQ_DELETE_RANGE =      138;
    public static final int     REQ_RENAME_INSTRUMENTS = 139;
    public static final int     REQ_COMPILE_QQL        = 140;
    public static final int     REQ_DESCRIBE           = 141;
    public static final int     REQ_LIST_STREAM_SPACES = 142;
    public static final int     REQ_LIST_SYMBOLS_FOR_SPACE = 143;
    public static final int     REQ_GET_TIME_RANGE_FOR_SPACE = 144;
    public static final int     REQ_PURGE_STREAM_SPACE = 145;
    public static final int     REQ_LIST_IDS_FOR_SPACE = 146;
    public static final int     REQ_DELETE_SPACES = 147;
    public static final int     REQ_RENAME_SPACES = 148;
    public static final int     REQ_DESCRIBE_QUERY = 149;

    public static final int     REQ_CREATE_STREAM =     200;
    public static final int     REQ_DELETE_STREAM =     201;
    public static final int     REQ_RENAME_STREAM =     202;
    public static final int     REQ_RUN_TASK =          203;
    public static final int     REQ_CREATE_FILE_STREAM =204;
    public static final int     REQ_CREATE_TOPIC =      205;
    public static final int     REQ_DELETE_TOPIC =      206;
    public static final int     REQ_CREATE_TOPIC_PUBLISHER = 207;
    public static final int     REQ_CREATE_TOPIC_SUBSCRIBER = 208;
    public static final int     REQ_LIST_TOPICS =       209;
    public static final int     REQ_GET_TOPIC_METADATA = 210;
    public static final int     REQ_CREATE_MULTICAST_TOPIC = 211;
    public static final int     REQ_CREATE_CUSTOM_TOPIC = 212;
    public static final int     REQ_GET_TOPIC_INSTRUMENT_MAPPING = 213; // Returns current snapshot of topic instrument mapping
    public static final int     REQ_CREATE_TOPIC_SUBSCRIBER_NO_MAPPING = 214; // Same as REQ_CREATE_TOPIC_SUBSCRIBER but not sends mapping to client
    public static final int     REQ_GET_TOPIC_TEMPORARY_INSTRUMENT_MAPPING = 215; // Returns current snapshot of topic temporary instrument mapping

    //  Download sub-protocol
    public static final int     REQ_CREATE_CURSOR =     1001;
    public static final int     REQ_CREATE_MULTICAST_CURSOR = 1002;

    //  Cursor client sends the following (16-bit int):
    public static final int     CURREQ_DISCONNECT =     2;
    public static final int     CURREQ_ADD_STREAMS =    10;
    public static final int     CURREQ_REMOVE_STREAMS = 11;
    public static final int     CURREQ_REMOVE_ALL_STREAMS = 12;
    public static final int     CURREQ_ALL_ENTITIES =   20;
    public static final int     CURREQ_ADD_ENTITIES =   21;
    public static final int     CURREQ_REMOVE_ENTITIES = 22;
    public static final int     CURREQ_CLEAR_ENTITIES = 23;
    public static final int     CURREQ_ALL_TYPES =      30;
    public static final int     CURREQ_ADD_TYPES =      31;
    public static final int     CURREQ_REMOVE_TYPES =   32;
    public static final int     CURREQ_CLEAR_TYPES =    33;
    public static final int     CURREQ_SET_TYPES =      34;
    public static final int     CURREQ_RESET_TIME =     40;
    //public static final int     CURREQ_RESET_FILTER =   41;
    //public static final int   CURREQ_RESET_TIME_AND_FILTER = 42;
    public static final int     CURREQ_RESET_INSTRUMENTS = 43;
    public static final int     CURREQ_ADD_ENTITIES_TYPES = 44;
    public static final int     CURREQ_REMOVE_ENTITIES_TYPES = 45;
    public static final int     CURREQ_GET_MULTICAST_CURSOR_METADATA = 46;
    //
    //  Cursor client receives the following (as message size).
    //  No message packet can be smaller than 10 bytes, so
    //  we have space for 10 codes.
    //  10 = 3 (min for time) + 4 (entity) + 2 (stream) + 1 (type)
    //
    public static final int     CURRESP_END_OF_CURSOR = 0;
    public static final int     CURRESP_ACK_SERIAL =    1;
    //public static final int     CURRESP_LOSS_REPORT =   2;
    public static final int     CURRESP_ERROR =         4;
    public static final int     CURRESP_LOAD_TYPE =     5;
    public static final int     CURRESP_LOAD_ENTITY =   6;
    public static final int     CURRESP_LOAD_STREAM =   7;
    public static final int     CURRESP_MSG =           8; // Aeron only
    public static final int     CURRESP_MSG_MULTIPART_HEAD = 9; // Aeron only
    public static final int     CURRESP_MSG_MULTIPART_BODY = 10; // Aeron only

    //  Upload sub-protocol
    public static final int     REQ_OPEN_LOADER =       2000;
    public static final int     LOAD_CLOSE =            0;
    public static final int     LOAD_REMOVE =           1;
    public static final int     LOAD_FLUSH =            2;
    public static final int     LOAD_MSG =              3;
    public static final int     LOAD_MULTIPART_HEAD =   4; // First part of multi-part message
    public static final int     LOAD_MULTIPART_HEAD_REMOVE =  5; // Same as LOAD_REMOVE for multipart message
    public static final int     LOAD_MULTIPART_BODY =   6; // Remaining parts for multi-part messages
    
    public static final int     LOADRESP_ERROR =        10;
    public static final int     LOADRESP_FILTER_CHANGE = 11;
    public static final int     LOADRESP_ENTITIES_CHANGE = 12;
    public static final int     LOADRESP_TYPES_CHANGE = 13;
    public static final int     LOADRESP_CLOSE_OK =     14;
    public static final int     LOADRESP_FLUSH_OK =     15;

    public static final int     RESP_OK =               0;
    public static final int     RESP_ERROR =            1;
    public static final int     RESP_LICENSE_ERROR =    3;
    public static final int     RESP_EXCEPTION =        4;

    // Session sub-protocol
    public static final int     REQ_START_SESSION =         3001;
    public static final int     REQ_GET_STREAMS =           1;
    public static final int     REQ_GET_STREAM_PROPERTY =   2;
    public static final int     REQ_CLOSE_SESSION =         3;

    public static final int     STREAM_PROPERTY_CHANGED =   11;
    public static final int     STREAM_DELETED =            12;
    public static final int     STREAM_CREATED =            13;
    public static final int     STREAM_RENAMED =            14;
    public static final int     STREAMS_DEFINITION =        15;
    public static final int     STREAM_PROPERTY =           16;
    public static final int     SESSION_CLOSED =            17;
    public static final int     SESSION_STARTED =           18;
    public static final int     STREAMS_CHANGED =           19;

    // Additional flags
    // TODO: Convert to enum
    public static final int     AF_STUB_STREAM =            11;

    public static final byte TRANSPORT_TYPE_SOCKET = 1;
    public static final byte TRANSPORT_TYPE_AERON  = 2;

    //returns NonSSL port for loopback connections while server is running with SSL
    public static int                   getSSLPort(int port) {
        int sum = 0;
        int n = port;
        while (n > 0) {
            sum += n % 10;
            n /= 10;
        }
        return port + sum + 1;
    }

    public static int                   getPort(int port, boolean ssl) {
        return ssl ? getSSLPort(port) : port;
    }

    public static String                getHttpProtocol(boolean ssl) {
        return ssl ? "https" : "http";
    }

    public static String                getProtocol(boolean ssl) {
        return ssl ? SSL_PROTOCOL_ID : PROTOCOL_ID;
    }

    public static boolean               isSSL(String protocol) {
        return protocol.equalsIgnoreCase(SSL_PROTOCOL_ID);
    }

    public static void                  writeParameters (
        Parameter []                        params,
        DataOutputStream                    out
    )
        throws IOException
    {
        out.writeInt(params != null ? params.length : -1);

        for (int i = 0; params != null && i < params.length; i++) {
            Parameter param = params[i];

            out.writeUTF(param.name);
            param.type.writeTo(out);
            out.writeUTF(param.value.getString());
        }
    }

    public static Parameter []          readParameters (DataInputStream in, int clientVersion)
            throws IOException
    {
        int size = in.readInt();
        if (size < 0)
            return null;

        Parameter[] params = new Parameter[size];
        for (int i = 0; i < size; i++) {
            String name = in.readUTF();
            DataType type = DataType.readFrom(in, null);// assuming we have primitive types here
            params[i] = new Parameter(name, type);
            params[i].value.writeString(in.readUTF());
        }

        return params;
    }

    public static void                  writeTransformationTask (
            TransformationTask                  task,
            DataOutputStream                    out)
        throws IOException
    {
        try {
            out.writeUTF (task.getClass().getName());

            if (task instanceof SerializableTask) {
                ((SerializableTask)task).write(out);
            } else {
                Marshaller      m = TransformationContext.createMarshaller(task);
                StringWriter    s = new StringWriter ();
                m.marshal (task, s);

                writeHugeString(out, s.toString());
            }
        } catch (JAXBException x) {
            throw new RuntimeException (x);
        }
    }

    public static TransformationTask   readTransformationTask (DataInputStream in)
        throws IOException
    {
        try {
            String          className = in.readUTF ();
            TransformationTask task =
                    (TransformationTask) Class.forName(className).newInstance();

            if (task instanceof SerializableTask) {
                ((SerializableTask) task).read(in);
                return task;

            } else {
                StringBuilder sb = new StringBuilder();
                readHugeString(in, sb);

                Unmarshaller    u = TransformationContext.createUnmarshaller(task);

                return ((TransformationTask) u.unmarshal (new StringReader (sb.toString())));
            }
        } catch (Exception x) {
            throw new RuntimeException (x);
        }
    }

    public static void                  writeTimeRange (long[] range, DataOutputStream out) throws IOException {
        boolean isNull = (range == null);
        out.writeBoolean(isNull);
        if (!isNull) {
            out.writeLong(range[0]);
            out.writeLong(range[1]);
        }
    }

    public static void                  writeTimeRange (TimeInterval range, DataOutputStream out) throws IOException {
        boolean isNull = range == null || range.isUndefined();
        out.writeBoolean(isNull);
        if (!isNull) {
            out.writeLong(TimeStamp.getMilliseconds(range.getFromTime()));
            out.writeLong(TimeStamp.getMilliseconds(range.getToTime()));
        }
    }

    public static long[]                readTimeRangeLong (DataInputStream in) throws IOException {
        boolean isNull = in.readBoolean();
        if (!isNull)
            return new long[] { in.readLong(), in.readLong() };
        return null;
    }

    public static void                  writeBGProcessInfo(BackgroundProcessInfo info, DataOutputStream out)
            throws IOException
    {
        if (info != null) {
            out.writeInt(info.status.ordinal());
            IOUtil.writeUTF(info.getName(), out);
            out.writeLong(info.startTime);
            out.writeLong(info.endTime);
            out.writeDouble(info.progress);
            out.writeInt(info.affectedStreams.size());
            for (String key : info.affectedStreams)
                IOUtil.writeUTF(key, out);

            out.writeBoolean(info.error != null);
            if (info.error != null)
                TDBProtocol.writeError(out, info.error);
        } else {
            out.writeInt(-1);
        }
    }

    public static BackgroundProcessInfo readBGProcessInfo(DataInputStream in) throws IOException, ClassNotFoundException {

        int ordinal = in.readInt();
        if (ordinal != -1) {
            BackgroundProcessInfo process = new BackgroundProcessInfo(in.readUTF());
            process.startTime = in.readLong();
            process.endTime = in.readLong();
            process.progress = in.readDouble();
            process.status = ExecutionStatus.values()[ordinal];
            for (int i = 0, length = in.readInt(); i < length; i++)
                process.affectedStreams.add(in.readUTF());

            boolean hasError = in.readBoolean();
            if (hasError)
                process.error = TDBProtocol.readError(in);

            return process;
        }

        return null;
    }

    public static void                  writeClassSet (
        DataOutputStream                    out,
        ClassSet                            md
    )
        throws IOException
    {
        try {
            Marshaller      m = UHFJAXBContext.createMarshaller ();

            StringWriter    s = new StringWriter ();

            synchronized (md) {
                m.marshal (md, s);
            }

            writeHugeString (out, s.getBuffer ());
        } catch (JAXBException x) {
            throw new RuntimeException (x);
        }
    }

    public static String                  toString (RecordClassSet md) throws IOException {
        try {
            Marshaller      m = UHFJAXBContext.createMarshaller ();

            StringWriter    s = new StringWriter ();
            m.marshal (md, s);
            return s.getBuffer().toString();
        } catch (JAXBException x) {
            throw new RuntimeException (x);
        }
    }

    public static RecordClassSet                  readClassSet (String in) {
        try {
            Unmarshaller    u = UHFJAXBContext.createUnmarshaller ();

            return ((RecordClassSet) u.unmarshal (new StringReader (in)));
        } catch (JAXBException x) {
            throw new RuntimeException (x);
        }
    }

    public static ClassSet                  readClassSet (
            DataInputStream                     in
    )
            throws IOException
    {
        try {
            StringBuilder   sb = new StringBuilder ();

            readHugeString (in, sb);

            Unmarshaller    u = UHFJAXBContext.createUnmarshaller ();

            return ((ClassSet) u.unmarshal (new StringReader (sb.toString ())));
        } catch (JAXBException x) {
            throw new RuntimeException (x);
        }
    }

//    public static void                  writeTimeRange(DataOutputStream out, DXTickStream stream, IdentityKey[] ids) throws IOException {
//        out.writeInt(ids == null ? -1 : ids.length);
//
//        if (ids != null) {
//
//            if (stream instanceof SingleChannelStream) {
//                TimeInterval[] intervals = ((SingleChannelStream) stream).listTimeRange(ids);
//
//                for (int i = 0; i < ids.length; i++) {
//                    writeIdentityKey(ids[i], out);
//
////                    if (Assertions.ENABLED) {
////                        if (intervals != null) {
////                            long[] range = stream.getTimeRange(ids[i]);
////
////                            if (intervals[i] == null)
////                                assert range == null;
////
////                            if (range == null) {
////                                if (intervals[i] != null) {
////                                    assert intervals[i].getFromTime() == Long.MAX_VALUE;
////                                    assert intervals[i].getToTime() == Long.MIN_VALUE;
////                                }
////                            } else {
////                                assert range[0] == TimeStamp.getMilliseconds(intervals[i].getFromTime());
////                                assert range[1] == TimeStamp.getMilliseconds(intervals[i].getToTime());
////                            }
////                        }
////                    }
//
//                    writeTimeRange(intervals != null ? intervals[i] : null, out);
//                    out.writeBoolean(false);
//                }
//            } else {
//
//                for (int i = 0; i < ids.length; i++) {
//                    IdentityKey id = ids[i];
//                    writeIdentityKey(id, out);
//                    long[] range = stream.getTimeRange(id);
//                    writeTimeRange(range, out);
//
//                    if (stream instanceof FriendlyStream && range != null)
//                        out.writeBoolean(((FriendlyStream)stream).hasWriter(id));
//                }
//            }
//
//        }
//    }

    public static void                  writeTimeRange(DataOutputStream out, DXTickStream stream, IdentityKey[] ids) throws IOException {
        out.writeInt(ids == null ? -1 : ids.length);

        FriendlyStream fs = stream instanceof FriendlyStream ? (FriendlyStream)stream : null;

        if (ids != null) {
            TimeInterval[] intervals = stream.listTimeRange(ids);

            for (int i = 0; i < ids.length; i++) {

//                    if (Assertions.ENABLED) {
//                        if (intervals != null) {
//                            long[] range = stream.getTimeRange(ids[i]);
//
//                            if (intervals[i] == null)
//                                assert range == null;
//
//                            if (range == null) {
//                                if (intervals[i] != null) {
//                                    assert intervals[i].getFromTime() == Long.MAX_VALUE;
//                                    assert intervals[i].getToTime() == Long.MIN_VALUE;
//                                }
//                            } else {
//                                assert range[0] == TimeStamp.getMilliseconds(intervals[i].getFromTime());
//                                assert range[1] == TimeStamp.getMilliseconds(intervals[i].getToTime());
//                            }
//                        }
//                    }

                writeIdentityKey(ids[i], out);
                TimeInterval interval = intervals != null ? intervals[i] : null;
                writeTimeRange(interval, out);

                if (interval != null && !interval.isUndefined())
                    out.writeBoolean(fs != null && fs.hasWriter(ids[i]));
            }
        }
    }

    public static void                  writeStream(DataOutputStream out, DXTickStream stream, int clientVersion)
            throws IOException
    {
        assert stream != null;

        out.writeUTF(stream.getKey());
        writeStreamOptions(out, stream.getStreamOptions(), clientVersion);
        writeTimeRange(out, stream, stream.listEntities());

        writeTimeRange(stream.getTimeRange(), out);
        writeBGProcessInfo(stream.getBackgroundProcess(), out);

        out.writeLong(stream.getDataVersion());
        out.writeLong(stream.getReplicaVersion());
        out.writeInt(stream.getFormatVersion());
    }

    public static void                  writeStreamOptions (
        DataOutputStream                    out,
        StreamOptions                       so,
        int                                 clientVersion
    )
        throws IOException
    {
        out.writeByte (so.scope.ordinal ());
        writeNullableString(so.name, out);
        writeNullableString(so.description, out);
        writeNullableString(so.location, out);
        out.writeInt (so.distributionFactor);
        out.writeBoolean(so.unique);
        out.writeBoolean(so.duplicatesAllowed);
        out.writeBoolean (so.isPolymorphic ());
        writeClassSet (out, so.getMetaData ());

        // write options
        out.writeBoolean(so.bufferOptions != null);
        if (so.bufferOptions != null) {
            out.writeInt(so.bufferOptions.initialBufferSize);
            out.writeInt(so.bufferOptions.maxBufferSize);
            out.writeLong(so.bufferOptions.maxBufferTimeDepth);
            out.writeBoolean(so.bufferOptions.lossless);
        }
        
        out.writeBoolean (so.periodicity == null);
        if (so.periodicity != null)
            out.writeUTF (so.periodicity.toString ());
        
        out.writeBoolean (so.highAvailability);        
        writeNullableString(so.distributionRuleName, out);
        if (clientVersion == 98)
            out.writeBoolean(false); // file format

        writeNullableString(so.owner, out);
        writeNullableString(so.version, out);

        if (clientVersion > 100) {
            // Write additional flags
            int additionalFlagCount = so.additionalFlags == null ? 0 : so.additionalFlags.size();
            out.writeInt(additionalFlagCount);
            if (additionalFlagCount > 0) {
                int[] ints = so.additionalFlags.toArray(new int[additionalFlagCount]);
                for (int i = 0; i < additionalFlagCount; i++)
                    out.writeInt(ints[i]);
            }
        }
    }

    public static StreamOptions         readStreamOptions (
        DataInputStream                     in,
        int                                 clientVersion
    )
        throws IOException
    {
        StreamOptions       so = new StreamOptions ();

        so.scope = StreamScope.values () [in.readUnsignedByte ()];
        so.name = readNullableString(in);
        so.description = readNullableString(in);
        so.location = readNullableString(in);
        so.distributionFactor = in.readInt ();
        so.unique = in.readBoolean();
        so.duplicatesAllowed = in.readBoolean();
        //in.readInt(); // so.notificationsDelay removed
        
        boolean             polymorphic = in.readBoolean ();
        RecordClassSet      md = (RecordClassSet) readClassSet (in);

        so.setMetaData (polymorphic, md);

        so.bufferOptions = in.readBoolean() ? new BufferOptions() : null;
        if (so.bufferOptions != null) {
            so.bufferOptions.initialBufferSize = in.readInt();
            so.bufferOptions.maxBufferSize = in.readInt();
            so.bufferOptions.maxBufferTimeDepth = in.readLong();
            so.bufferOptions.lossless = in.readBoolean();
        }

        so.periodicity = in.readBoolean() ? null : Periodicity.parse(in.readUTF());
        so.highAvailability = in.readBoolean ();
        so.distributionRuleName = readNullableString(in);

        if (clientVersion < 99) {
            boolean exists = in.readBoolean();
            if (exists)
                in.readByte();
        }

        so.owner = readNullableString(in);
        so.version = readNullableString(in);

        if (clientVersion > 100) {
            // Read additional flags
            int additionalFlagCount = in.readInt();
            if (additionalFlagCount > 0) {
                so.additionalFlags = new IntegerHashSet();
                for (int i = 0; i < additionalFlagCount; i++) {
                    int flag = in.readInt();
                    so.additionalFlags.add(flag);
                }
            }
        }
        
        return (so);
    }

    public static StreamOptions         readStreamOptions76 (
            DataInputStream                     in
    )
            throws IOException
    {
        StreamOptions       so = new StreamOptions ();

        so.scope = StreamScope.values () [in.readUnsignedByte ()];
        so.name = readNullableString(in);
        so.description = readNullableString(in);
        so.distributionFactor = in.readInt ();
        so.unique = in.readBoolean();
        so.duplicatesAllowed = in.readBoolean();
        in.readInt(); // notification delay

        boolean             polymorphic = in.readBoolean ();
        RecordClassSet      md = (RecordClassSet) readClassSet (in);

        so.setMetaData (polymorphic, md);

        so.bufferOptions = in.readBoolean() ? new BufferOptions() : null;
        if (so.bufferOptions != null) {
            so.bufferOptions.initialBufferSize = in.readInt();
            so.bufferOptions.maxBufferSize = in.readInt();
            so.bufferOptions.maxBufferTimeDepth = in.readLong();
            so.bufferOptions.lossless = in.readBoolean();
        }

        so.periodicity = in.readBoolean() ? null : Periodicity.parse(in.readUTF());
        so.highAvailability = in.readBoolean ();

        return (so);
    }

    public static void                  writeNonNullableStrings (
        DataOutputStream                    out,
        String []                           strings
    )
        throws IOException
    {
        int         n = strings.length;

        if (n > 0xFFFF)
            throw new IllegalArgumentException ("Array too big: " + n);

        out.writeShort (n);
        
        for (String s : strings)
            out.writeUTF (s);
    }

    public static String []             readNonNullableStrings (
        DataInputStream                     in
    )
        throws IOException
    {
        int             n = in.readUnsignedShort ();
        String []       ret = new String [n];

        for (int ii = 0; ii < n; ii++)
            ret [ii] = in.readUTF ();

        return (ret);
    }

    public static void writeNullableStringArray(DataOutputStream out, @Nullable String[] strings) throws IOException {
        if (strings == null) {
            out.writeShort(-1);
            return;
        }
        int size = strings.length;
        if (size > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Array too big: " + size);
        }

        out.writeShort(size);
        for (String s : strings) {
            out.writeUTF(s);
        }
    }

    public static void writeNullableStringArray(DataOutputStream out, @Nullable List<CharSequence> strings) throws IOException {
        if (strings == null) {
            out.writeShort(-1);
            return;
        }

        int size = strings.size();
        if (size > Short.MAX_VALUE)
            throw new IllegalArgumentException("Array too big: " + size);

        out.writeShort(size);
        for (CharSequence s : strings)
            out.writeUTF(s.toString());
    }

    @Nullable
    public static String[] readNullableStringArray(DataInputStream din) throws IOException {
        int size = din.readShort();
        if (size < 0) {
            return null;
        }
        String[] result = new String[size];
        for (int i = 0; i < size; i++) {
            result[i] = din.readUTF();
        }
        return result;
    }
    
    public static void                  writeCredentials(VSChannel channel, UserPrincipal principal)
            throws IOException
    {
        DataOutputStream out = channel.getDataOutputStream();
        out.writeUTF("BASIC"); // protocol type
        out.writeUTF(channel.encode(principal.getToken()));
    }

    public static void                  writeCredentials(VSChannel channel, UserPrincipal principal, Principal delegate)
            throws IOException
    {
        if (delegate == null) {
            writeCredentials(channel, principal);
        } else {
            DataOutputStream out = channel.getDataOutputStream();
            out.writeUTF("DELEGATE"); // protocol type
            out.writeUTF(channel.encode(principal.getToken()));
            out.writeUTF(delegate.getName());
        }
    }

    /*
    *   Writes object as binary into given DataOutputStream and do flush()
     */

    public static void                  writeBinary(DataOutputStream out, Throwable e) throws IOException {
        ObjectOutputStream      oos = new ObjectOutputStream (out);
        oos.writeObject (e);
        out.flush();
    }

    @SuppressFBWarnings(value = "OBJECT_DESERIALIZATION", justification = "Only Throwable Deserialization")
    public static Throwable                readBinary(DataInputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream       ois = new ObjectInputStream (in);
        return (Throwable) ois.readObject();
    }

    /*
    *   Writes Throwable into text presentation into given DataOutputStream and do flush()
     */

    public static void                  writeError(DataOutputStream out, Throwable e) throws IOException {
        out.writeUTF(e.getClass().getName());
        out.writeUTF(e.getMessage() != null ? e.getMessage() : "");

        StackTraceElement[] trace = e.getStackTrace();
        out.writeInt(trace.length);
        for (StackTraceElement element : trace) {
            out.writeUTF(element.getClassName());
            out.writeUTF(element.getMethodName());

            String file = element.getFileName();
            out.writeUTF(file != null ? file : "");
            out.writeInt(element.getLineNumber());

        }
        out.writeBoolean(e.getCause() != null);
        if (e.getCause() != null)
            writeError(out, e.getCause());

        out.flush();
    }

    public static Throwable             readError(DataInputStream in) throws IOException, ClassNotFoundException {
        return readError(in, DefaultExceptionResolver.INSTANCE);
    }

    public static Throwable             readError(DataInputStream in, ExceptionResolver resolver)
            throws IOException, ClassNotFoundException {

        String className = in.readUTF();
        String message = in.readUTF();

        int size = in.readInt();

        StackTraceElement[] trace = new StackTraceElement[size];
        for (int i = 0; i < size; i++) {
            String clazz = in.readUTF();
            String method = in.readUTF();
            String file = in.readUTF();
            int line = in.readInt();
            trace[i] = new StackTraceElement(clazz, method, file, line);
        }

        Throwable cause = null;
        if (in.readBoolean())
            cause = readError(in, resolver);

        Throwable x = resolver.create(className, message, cause);
        x.setStackTrace(trace);
        return x;
    }

    public static UserCredentials           readCredentials(VSChannel channel) throws IOException {
        DataInputStream in = channel.getDataInputStream();
        String protocol = in.readUTF();
        String token = channel.decode(in.readUTF());

        String delegate = "DELEGATE".equals(protocol) ? in.readUTF() : null;

        if (!StringUtils.isEmpty(token)) {
            String[] tokens = token.split(":");
            UserCredentials c = new UserCredentials(protocol, tokens[0], tokens.length > 1 ? tokens[1] : "");
            c.delegate = delegate;
            return c;
        }

        return null;
    }
}