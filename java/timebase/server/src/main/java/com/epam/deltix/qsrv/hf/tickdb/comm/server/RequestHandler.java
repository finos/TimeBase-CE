/*
 * Copyright 2024 EPAM Systems, Inc
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

import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import com.epam.deltix.qsrv.hf.tickdb.comm.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.qsrv.hf.topic.DirectProtocol;
import com.google.common.collect.ImmutableList;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.hf.tickdb.server.Version;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.qsrv.hf.pub.md.MetaData;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.download.multicast.DownloadHandlerFactoryMulticast;
import com.epam.deltix.qsrv.hf.tickdb.impl.FriendlyStream;
import com.epam.deltix.qsrv.hf.tickdb.impl.ServerLock;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBWrapper;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicPublisherRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicPublisherResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicSubscriberRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicSubscriberResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CopyTopicToStreamTaskManager;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CreateCustomTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CreateMulticastTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CreateTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.DeleteTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.GetTopicMetadataRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.GetTopicMetadataResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.ListTopicsResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicTransferType;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.CreateTopicResult;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.LoaderSubscriptionResult;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.ReaderSubscriptionResult;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.TopicChannelOptionMap;
import com.epam.deltix.qsrv.hf.tickdb.lang.parser.QQLParser;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TextMap;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Token;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicApiException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.io.ByteArrayOutputStreamEx;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.security.TimebaseAccessController;
import com.epam.deltix.util.time.Periodicity;
import com.epam.deltix.util.vsocket.ChannelClosedException;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSChannelState;
import com.epam.deltix.util.vsocket.VSServerFramework;
import io.aeron.Aeron;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.AuthenticationException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 *
 */
public class RequestHandler extends QuickExecutor.QuickTask {
    private static final Log LOG = LogFactory.getLog(RequestHandler.class);

    private final DXTickDB                  tickDB;
    private final Map<String, DXTickDB>     userNameToDb;
    private DXTickDB                        db;

    private final ServerParameters          params;

    private final SecurityContext           context;
    private VSChannel                       ds;
    private final DataOutputStream          out;
    private Principal                       user;
    private int                             clientVersion;


    private final DXServerAeronContext      aeronContext;
    private final AeronThreadTracker        aeronThreadTracker;
    private final DirectTopicRegistry       topicRegistry;

    private final ByteArrayOutputStreamEx   buffer = new ByteArrayOutputStreamEx(1024);
    private final DataOutputStream          dout = new DataOutputStream(buffer);

    // TODO: Consider refactoring
    
    public RequestHandler(VSChannel ds, DXTickDB db,
                          Map<String, DXTickDB> userNameToDb,
                          ServerParameters params,
                          QuickExecutor exe, SecurityContext context,
                          @Nonnull DXServerAeronContext aeronContext,
                          @Nonnull AeronThreadTracker aeronThreadTracker, DirectTopicRegistry topicRegistry) {
        super (exe);
        this.ds = ds;
        this.tickDB = db;
        this.userNameToDb = userNameToDb;
        this.params = params;
        this.context = context;
        this.out = ds.getDataOutputStream ();
        this.aeronContext = aeronContext;
        this.aeronThreadTracker = aeronThreadTracker;
        this.topicRegistry = topicRegistry;
    }
    
    public void                 run () throws InterruptedException {
        int                 req = -1;

        try {
            clientVersion = ds.getDataInputStream ().readInt ();
            UserCredentials credentials = TDBProtocol.readCredentials(ds);

            req = ds.getDataInputStream ().readInt ();

            if (clientVersion < TDBProtocol.MIN_CLIENT_VERSION) {
                if (req == TDBProtocol.REQ_CONNECT)
                    doRejectConnection (ds);
                else
                    TickDBServer.LOGGER.severe (
                        "Incompatible client PV#" + clientVersion +
                        "; apparently it ignored handshake rejection."
                    );
            }

            try {
                db = tickDB;

//                if (!LicenseController.qs().isLicensed ()) {
//                    TickDBServer.LOGGER.severe ("Unlicensed; request aborted.");
//
//                    out.writeInt (TDBProtocol.RESP_LICENSE_ERROR);
//                    out.flush();
//                }
                {
                    if (context != null) {
                        if (credentials != null) {
                            user = context.authenticate(credentials);
                            synchronized (userNameToDb) {
                                db = userNameToDb.get(user.getName());
                                if (db == null)
                                    userNameToDb.put(user.getName(), (db = new TickDBWrapper(tickDB, context.controller, user)));
                            }
                        } else
                            throw new AuthenticationException("User is not specified.");
                    }

                    if (req == TDBProtocol.REQ_CONNECT)
                        UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.CONNECT_PATTERN);

                    switch (req) {
                        case TDBProtocol.REQ_CONNECT:                   doConnect (ds);     break;
                        //case TDBProtocol.REQ_LIST_STREAMS:              doListStreams (ds); break;
                        case TDBProtocol.REQ_GET_MD_VERSION:            doGetMDVersion (ds); break;
                        case TDBProtocol.REQ_GET_METADATA:              doGetMetadata (ds); break;
                        case TDBProtocol.REQ_SET_METADATA:              doSetMetadata (ds); break;
                        case TDBProtocol.REQ_TRIM_TO_SIZE:              doTrimToSize (ds);  break;
                        case TDBProtocol.REQ_START_SESSION:
                            new SessionHandler(user, ds, db, context, executor, clientVersion);
                            ds = null;
                            break;
                        case TDBProtocol.REQ_CREATE_CURSOR:
                            startDownload (ds, clientVersion);
                            ds = null;
                            break;
                        case TDBProtocol.REQ_CREATE_MULTICAST_CURSOR:
                            startMulticastDownload (ds, clientVersion);
                            ds = null;
                            break;
                        case TDBProtocol.REQ_OPEN_LOADER:
                            doUpload (ds, clientVersion);
                            ds = null;
                            break;
                        case TDBProtocol.REQ_LOCK_STREAM:               doLockStream (ds); break;
                        case TDBProtocol.REQ_TRY_LOCK_STREAM:           doTryLockStream (ds); break;
                        case TDBProtocol.REQ_UNLOCK_STREAM:             doUnlockStream (ds); break;
                        case TDBProtocol.REQ_GET_LOCK_STATE:            doGetLockState (ds); break;
                        
                        case TDBProtocol.REQ_CREATE_STREAM:             doCreateStream (ds); break;
                        //case TDBProtocol.REQ_CREATE_FILE_STREAM:        doCreateFileStream (ds); break;
                        case TDBProtocol.REQ_DELETE_STREAM:             doDeleteStream (ds); break;
                        case TDBProtocol.REQ_RENAME_STREAM:             doRenameStream (ds); break;
                        case TDBProtocol.REQ_GET_STREAM:                doGetStream(ds); break;
                        case TDBProtocol.REQ_GET_STREAM_DESCR:          doGetStreamDescr (ds); break;
                        case TDBProtocol.REQ_SET_STREAM_DESCR:          doSetStreamDescr (ds); break;
                        case TDBProtocol.REQ_GET_STREAM_NAME:           doGetStreamName (ds); break;
                        case TDBProtocol.REQ_SET_STREAM_NAME:           doSetStreamName (ds); break;
                        case TDBProtocol.REQ_GET_STREAM_TNF:            doGetStreamTNF (ds); break;
                        case TDBProtocol.REQ_GET_STREAM_PERIOD:         doGetStreamPeriod (ds); break;
                        case TDBProtocol.REQ_SET_STREAM_PERIOD:         doSetStreamPeriod (ds); break;
                        case TDBProtocol.REQ_GET_STREAM_OPTIONS:        doGetStreamOptions (ds); break;
                        case TDBProtocol.REQ_SET_STREAM_HA:             doSetHighAvailability(ds); break;
                        case TDBProtocol.REQ_GET_STREAM_HA:             doGetHighAvailability(ds); break;
                        case TDBProtocol.REQ_ENABLE_VERSIONING:         doEnableVersioning(ds); break;
                        case TDBProtocol.REQ_GET_DATA_VERSION:          doGetDataVersion(ds); break;
                        case TDBProtocol.REQ_GET_REPLICA_VERSION:       doGetReplicaVersion(ds); break;
                        case TDBProtocol.REQ_SET_REPLICA_VERSION:       doSetReplicaVersion(ds); break;
                        case TDBProtocol.REQ_GET_INSTR_COMPOSITION:     doGetComposition(ds); break;
                        case TDBProtocol.REQ_GET_TIME_RANGE:            doGetTimeRange (ds); break;
                        case TDBProtocol.REQ_LIST_TIME_RANGE:           doListTimeRange (ds); break;
                        case TDBProtocol.REQ_LIST_ENTITIES:             doListEntities (ds); break;
                        case TDBProtocol.REQ_CLEAR_DATA:                doClearData(ds); break;
                        case TDBProtocol.REQ_TRUNCATE_DATA:             doTruncateData(ds); break;
                        case TDBProtocol.REQ_PURGE_STREAM:              doPurge(ds); break;
                        case TDBProtocol.REQ_PURGE_STREAM_SPACE:        doPurgeSpace(ds); break;
                        case TDBProtocol.REQ_DELETE_RANGE:              doDeleteStreamRange(ds); break;
                        case TDBProtocol.REQ_GET_SERVER_TIME:           doGetServerTime(ds); break;
                        case TDBProtocol.REQ_GET_BG_PROCESS:            doGetBackgroundProcess(ds); break;
                        case TDBProtocol.REQ_ABORT_BG_PROCESS:          doAbortBackgroundProcess(ds); break;
                        case TDBProtocol.REQ_RUN_TASK:                  doRunTransformation(ds); break;
                        case TDBProtocol.REQ_SET_STREAM_TYPE:           doSetStreamType (ds);    break;
                        case TDBProtocol.REQ_GET_SIZE:                  doGetSize (ds);  break;
                        case TDBProtocol.REQ_GET_STREAM_TYPE:           doGetStreamType (ds);    break;
                        case TDBProtocol.REQ_GET_STREAM_TYPE_VERSION:   doGetStreamTypeVersion (ds);    break;
                        case TDBProtocol.REQ_SET_STREAM_OWNER:          doSetStreamOwner(ds); break;
                        case TDBProtocol.REQ_RENAME_INSTRUMENTS:        doRenameInstruments(ds); break;
                        case TDBProtocol.REQ_COMPILE_QQL:               doCompileQQL(ds); break;
                        case TDBProtocol.REQ_DESCRIBE:                  doDescribeStream(ds); break;
                        case TDBProtocol.REQ_LIST_STREAM_SPACES:        doListSpaces(ds); break;
                        case TDBProtocol.REQ_LIST_SYMBOLS_FOR_SPACE:    doListSymbolsForSpace(ds); break;
                        case TDBProtocol.REQ_LIST_IDS_FOR_SPACE:        doListIdsForSpace(ds); break;
                        case TDBProtocol.REQ_GET_TIME_RANGE_FOR_SPACE:  doGetTimeRangeForSpace(ds); break;
                        case TDBProtocol.REQ_DESCRIBE_QUERY:            doDescribeQQL(ds); break;

                        // TOPIC_SUB_PROTOCOL_VERSION
                        case TDBProtocol.REQ_CREATE_TOPIC:              doCreateIpcTopic(ds, clientVersion); break;
                        case TDBProtocol.REQ_CREATE_MULTICAST_TOPIC:    doCreateMulticastTopic(ds, clientVersion); break;
                        case TDBProtocol.REQ_CREATE_CUSTOM_TOPIC:       doCreateCustomTopic(ds, clientVersion); break;
                        case TDBProtocol.REQ_DELETE_TOPIC:              doDeleteTopic(ds, clientVersion); break;
                        case TDBProtocol.REQ_LIST_TOPICS:               doListTopics(ds, clientVersion); break;
                        case TDBProtocol.REQ_GET_TOPIC_METADATA:        doGetTopicMetadata(ds, clientVersion); break;
                        case TDBProtocol.REQ_CREATE_TOPIC_PUBLISHER:    doCreateTopicPublisher(ds, clientVersion); break;
                        case TDBProtocol.REQ_CREATE_TOPIC_SUBSCRIBER:   doCreateTopicSubscriber(ds, clientVersion); break;

                        default:
                            TickDBServer.LOGGER.warning ("Unrecognized request code: " + req);
                            break;
                    }
                }

                if (ds != null && ds.getState() == VSChannelState.Connected)
                    out.flush ();
            } catch (InterruptedException ix) {
                throw ix;
            } catch (UncheckedInterruptedException ix) {
                throw ix;
            } catch (ChannelClosedException ix) {
                // ok
            } catch (Throwable x) {
                boolean minorTopicException = x instanceof TopicApiException;

                LogLevel userLogLevel = minorTopicException ? LogLevel.DEBUG : LogLevel.WARN;
                UserLogger.log(userLogLevel, user, ds.getRemoteAddress(), ds.getRemoteApplication(), "Request handling error: " + x.getMessage(), x);

                if (x instanceof UnknownStreamException) {
                    if (req != TDBProtocol.REQ_GET_BG_PROCESS)
                        TickDBServer.LOGGER.log (Level.FINE, "Request handling error: " + x.getMessage());
                }
                else if (!(x instanceof StreamLockedException || minorTopicException)) {
                    TickDBServer.LOGGER.log (Level.SEVERE, "Request handling error; sending to client: ", x);
                }

                switch (req) {
                    case TDBProtocol.REQ_CREATE_CURSOR:
                        break;

                    default:
                        out.writeInt (TDBProtocol.RESP_ERROR);
                        TDBProtocol.writeError(out, x);
                        break;
                }
            }
        } catch (IOException iox) {
            TickDBServer.LOGGER.log (Level.SEVERE, "Request handling error processing request: " + req, iox);
        } finally {
            Util.close (ds);
        }
    }

    private void                doConnect (VSChannel ds) throws IOException {
        boolean isReadOnly = db.isReadOnly();

        out.writeInt (TDBProtocol.RESP_OK);
        out.writeInt (TDBProtocol.VERSION);
        out.writeByte(isReadOnly ? TDBProtocol.READ_ONLY : TDBProtocol.READ_WRITE);
        String version = Version.getVersion();
        out.writeUTF(version == null ? com.epam.deltix.util.Version.VERSION_STRING : version);
        out.writeBoolean(context != null);

        long bandwidth = params != null ? params.maxBandwidth : Long.MAX_VALUE;
        long available = bandwidth != Long.MAX_VALUE ?
                bandwidth - VSServerFramework.INSTANCE.getThroughput() : Long.MAX_VALUE;
        if (available < 0)
            TickDBServer.LOGGER.warning("Maximum bandwidth exceeded: " + -available);
        out.writeLong(available);

        if (UserLogger.canTrace(user))
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.CONNECTED_PATTERN);

//        if (context.ac != null)
//            context.ac.connected(user, ds.getRemoteAddress());

    }
    
    private void                doRejectConnection (VSChannel ds) throws IOException {
        out.writeInt (TDBProtocol.RESP_OK);
        out.writeInt (TDBProtocol.VERSION);
        out.writeByte (TDBProtocol.INCOMPATIBLE_CLIENT_PROTOCOL_VERSION);
        out.writeUTF (Version.getVersion());
    }

//    private void                doListStreams (VSChannel ds)
//        throws IOException
//    {
//        long                        clientsStreamVersion = ds.getDataInputStream().readLong ();
//        long                        serverStreamVersion = db.getStreamVersion ();
//
//        if (clientsStreamVersion == serverStreamVersion)
//            out.writeInt (TDBProtocol.NO_CHANGES);
//        else {
//            List<DXTickStream> streams = new ArrayList<DXTickStream>(Arrays.asList(db.listStreams()));
//
//            for (int i = streams.size() - 1; i >= 0; i--) {
//                if (!hasStreamAccess(streams.get(i).getKey()))
//                    streams.remove(i);
//            }
//
//            out.writeInt (streams.size());
//            for (int i = 0; i < streams.size(); i++) {
//                DXTickStream stream = streams.get(i);
//                out.writeUTF (stream.getKey());
//                out.writeByte (stream.getScope().ordinal ());
//            }
//
//            out.writeLong (0);
//        }
//    }

    private void                doGetMDVersion (VSChannel ds)
        throws IOException
    {
        long metaDataVersion = db.getMetaDataVersion();

        out.writeInt (TDBProtocol.RESP_OK);
        out.writeLong (metaDataVersion);
    }

     private void                doGetServerTime (VSChannel ds)
        throws IOException
    {
        long serverTime = db.getServerTime();

        out.writeInt (TDBProtocol.RESP_OK);
        out.writeLong (serverTime);
    }

    private void                doGetMetadata (VSChannel ds)
        throws IOException
    {
        MetaData                        md = db.getMetaData ();
        RecordClassSet                  rcs;

        if (md instanceof RecordClassSet)
            rcs = (RecordClassSet) md;
        else {
            rcs = new RecordClassSet ();
            rcs.setClassDescriptors (md.getClassDescriptors ());
        }

        out.writeInt (TDBProtocol.RESP_OK);        
        TDBProtocol.writeClassSet (out, rcs, clientVersion);
    }

    private void                doSetMetadata (VSChannel ds)
        throws IOException
    {
        RecordClassSet  rcs = (RecordClassSet) TDBProtocol.readClassSet (ds.getDataInputStream());

        MetaData        md = db.getMetaData ();

        md.setClassDescriptors (rcs.getClassDescriptors ());
        ds.getDataOutputStream().writeInt (TDBProtocol.RESP_OK);
    }

    private void                doTrimToSize (VSChannel ds) throws IOException {
        db.trimToSize ();
        ds.getDataOutputStream ().writeInt (TDBProtocol.RESP_OK);
    }
    
    private void                doGetSize (VSChannel ds) throws IOException {
        long                ret = db.getSizeOnDisk ();
        
        out.writeInt (TDBProtocol.RESP_OK);
        out.writeLong (ret);
    }

    private void                doDeleteStreamRange (VSChannel ds) throws IOException {
        final DXTickStream          stream = getStream (ds);

        ServerLock lock = readLock(ds);
        ((LockVerifier) stream).checkExclusiveWrite(lock);

        DataInputStream in = ds.getDataInputStream();

        long start = in.readLong();
        long end = in.readLong();
        IdentityKey[] ids = TDBProtocol.readInstrumentIdentities(in);

        stream.delete (TimeStamp.fromNanoseconds(start), TimeStamp.fromNanoseconds(end), ids);
        
        out.writeInt (TDBProtocol.RESP_OK);
    }

    private void                doDeleteStream (VSChannel ds) throws IOException {
        final DXTickStream          stream = getStream (ds);

        ServerLock lock = readLock(ds);
        ((LockVerifier) stream).checkExclusiveWrite(lock);

        String key = stream.getKey();

        stream.delete ();

        out.writeInt (TDBProtocol.RESP_OK);

        if (UserLogger.canTrace(user))
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.DELETE_STREAM_PATTERN, key);
    }

    private void                doRenameStream (VSChannel ds) throws IOException {
        final DXTickStream          stream = getStream (ds);
        String key = ds.getDataInputStream().readUTF ();

        ((LockVerifier) stream).checkExclusiveWrite(readLock(ds));
        stream.rename(key);

        if (UserLogger.canTrace(user))
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.RENAME_STREAM_PATTERN, key);

        out.writeInt (TDBProtocol.RESP_OK);
    }

    
    private void                doGetTimeRange (VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);
        final IdentityKey [] ids = TDBProtocol.readInstrumentIdentities (ds.getDataInputStream());

        final long []               tr = stream.getTimeRange (ids);        
        
        out.writeInt (TDBProtocol.RESP_OK);
        
        if (tr == null)
            out.writeLong (Long.MAX_VALUE);
        else {
            out.writeLong (tr [0]);
            out.writeLong (tr [1]);
        }                            
    }

    private void                doListTimeRange (VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);
        final IdentityKey [] ids = TDBProtocol.readInstrumentIdentities (ds.getDataInputStream());

        final ArrayList<long[]> ranges = new ArrayList<>();
        for (int i = 0; i < ids.length; ++i)
            ranges.add(stream.getTimeRange(ids[i]));

        out.writeInt (TDBProtocol.RESP_OK);

        for (int i = 0; i < ranges.size(); i++)
            TDBProtocol.writeTimeRange(ranges.get(i), out);
    }
    
    private void                doListEntities (VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);

        final IdentityKey [] ids = stream.listEntities ();
        
        out.writeInt (TDBProtocol.RESP_OK);
        
        TDBProtocol.writeInstrumentIdentities (ids, out);                          
    }

    private void                doRenameInstruments(VSChannel ds) throws IOException {
        final DXTickStream stream = getStream(ds);
        final DataInputStream is = ds.getDataInputStream();
        final IdentityKey[] from = TDBProtocol.readInstrumentIdentities(is);
        final IdentityKey[] to = TDBProtocol.readInstrumentIdentities(is);

        stream.renameInstruments(from, to);

        out.writeInt(TDBProtocol.RESP_OK);
    }

    private void                doDescribeQQL(VSChannel ds) throws IOException {
        final DataInputStream is = ds.getDataInputStream();
        String query = is.readUTF();

        SelectionOptions options = new SelectionOptions();
        SelectionOptionsCodec.read(is, options, clientVersion);
        Parameter[] parameters = TDBProtocol.readParameters(is, clientVersion);

        ClassSet set = db.describeQuery(query, options, parameters);

        out.writeInt(TDBProtocol.RESP_OK);
        TDBProtocol.writeClassSet(ds.getDataOutputStream(), set, clientVersion);
        out.flush();
    }

    private void                doCompileQQL(VSChannel ds) throws IOException {
        final DataInputStream is = ds.getDataInputStream();
        String query = is.readUTF();
        //Parameter[] parameters = TDBProtocol.readParameters(is); // will be nice to have

        CompilationException error = null;
        TextMap map = QQLParser.createTextMap();
        try {
            QQLParser.parse(query, map);
        } catch (CompilationException e) {
            error = e;
        }

        out.writeInt(TDBProtocol.RESP_OK);

        out.writeLong(error != null ? error.location : Long.MIN_VALUE);
        if (error != null)
            TDBProtocol.writeError(out, error);

        Token[] tokens = map.getTokens();
        out.writeInt(tokens.length);

        for (Token token : tokens) {
            out.writeLong(token.location);
            out.writeUTF(token.type.toString());
        }

        out.flush();
    }

    private void                doDescribeStream(VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);
        out.writeInt(TDBProtocol.RESP_OK);
        TDBProtocol.writeHugeString (out, stream.describe ());
    }

    private void                doListSpaces(VSChannel ds) throws IOException {
        final DXTickStream stream = getStream(ds);
        out.writeInt(TDBProtocol.RESP_OK);
        String[] spaces = stream.listSpaces();
        TDBProtocol.writeNullableStringArray(out, spaces);
    }

    private void                doListSymbolsForSpace(VSChannel ds) throws IOException {
        final DXTickStream stream = getStream(ds);
        final DataInputStream is = ds.getDataInputStream();
        String space = is.readUTF();
        out.writeInt(TDBProtocol.RESP_OK);

        List<CharSequence> symbols = Arrays.stream(stream.listEntities(space)).map(IdentityKey::getSymbol).collect(Collectors.toList());

        TDBProtocol.writeNullableStringArray(out, symbols);
    }

    private void                doListIdsForSpace(VSChannel ds) throws IOException {
        final DXTickStream stream = getStream(ds);
        final DataInputStream is = ds.getDataInputStream();
        String space = is.readUTF();
        out.writeInt(TDBProtocol.RESP_OK);
        IdentityKey[] ids = stream.listEntities(space);
        TDBProtocol.writeInstrumentIdentities (ids, out);
    }

    private void                doGetTimeRangeForSpace(VSChannel ds) throws IOException {
        final DXTickStream stream = getStream(ds);
        final DataInputStream is = ds.getDataInputStream();
        String space = is.readUTF();
        out.writeInt(TDBProtocol.RESP_OK);
        TDBProtocol.writeTimeRange(stream.getTimeRange(space), out);
    }
    
    private void                doCreateStream (VSChannel ds) throws IOException {

        DataInputStream     in = ds.getDataInputStream();
        String              key  = in.readUTF ();
        StreamOptions       so = TDBProtocol.readStreamOptions (in, clientVersion);

        db.createStream(key, so);
        out.writeInt (TDBProtocol.RESP_OK);

        if (UserLogger.canTrace(user))
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.CREATE_STREAM_PATTERN, key);
    }

//    private void                doCreateFileStream (VSChannel ds) throws IOException {
//
//        DataInputStream     in = ds.getDataInputStream();
//        String              key  = in.readUTF ();
//        String              dataFile  = in.readUTF ();
//
//        db.createFileStream(key, dataFile);
//        out.writeInt (TDBProtocol.RESP_OK);
//
//        if (UserLogger.canTrace(user))
//            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), UserLogger.CREATE_STREAM_PATTERN, key);
//    }
    
    private DXTickStream          getStream (VSChannel ds) throws IOException {
        String              key = ds.getDataInputStream().readUTF ();
        DXTickStream        stream = db.getStream (key);

        if (stream == null)
            throw new UnknownStreamException("Unknown stream: " + key);

        return (stream);
    }
    
    private void                doGetStreamOptions(VSChannel ds) throws IOException {
        final DXTickStream      stream = getStream (ds);

        StreamOptions options = stream.getStreamOptions();
        
        // make sure that we get options without exceptions
        out.writeInt (TDBProtocol.RESP_OK);
        TDBProtocol.writeStreamOptions(out, options, clientVersion);
    }
    
    private void                doGetStreamName (VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);
        final String name = stream.getName();
        
        out.writeInt (TDBProtocol.RESP_OK);
        TDBProtocol.writeNullableString (name, out);
    }

    private void                doSetStreamName (VSChannel ds) throws IOException {
        DXTickStream            stream = getStream (ds);
        final String name = TDBProtocol.readNullableString(ds.getDataInputStream());

        ((LockVerifier) stream).checkExclusiveWrite(readLock(ds));

        stream.setName(name);
        
        out.writeInt (TDBProtocol.RESP_OK);
    }
    
    private void                doGetStreamPeriod (VSChannel ds) throws IOException {
        final DXTickStream      stream = getStream (ds);
        final Periodicity v = stream.getPeriodicity();

        out.writeInt (TDBProtocol.RESP_OK);
        TDBProtocol.writeNullableString(v.toString(), out);
    }

    private void                doSetStreamPeriod (VSChannel ds) throws IOException {
        DXTickStream            stream = getStream (ds);
        final String v = TDBProtocol.readNullableString(ds.getDataInputStream());

        ((LockVerifier) stream).checkExclusiveWrite(readLock(ds));

        stream.setPeriodicity(Periodicity.parse(v));
        out.writeInt (TDBProtocol.RESP_OK);
    }

    private void                doSetHighAvailability (VSChannel ds) throws IOException {
        DXTickStream    stream = getStream (ds);
        final int       value = ds.getDataInputStream().readInt();

        stream.setHighAvailability(value == 1);
        out.writeInt (TDBProtocol.RESP_OK);
    }

    private void                doGetHighAvailability (VSChannel ds) throws IOException {
        final DXTickStream      stream = getStream (ds);
        final boolean highAvailability = stream.getHighAvailability();

        out.writeInt (TDBProtocol.RESP_OK);
        out.writeInt (highAvailability ? 1 : 0);
    }

    private void                doGetStreamType (VSChannel ds) throws IOException {
        final TickStream            stream = getStream (ds);
        RecordClassSet              md = getMetaData (stream);
        boolean isFixedType = stream.isFixedType();
        
        out.writeInt (TDBProtocol.RESP_OK);
        out.writeBoolean (isFixedType);
        TDBProtocol.writeClassSet (out, md, clientVersion);
    }
    
    private void                doGetStreamTypeVersion (VSChannel ds) throws IOException {
        final TickStream            stream = getStream (ds);
        long typeVersion = stream.getTypeVersion();

        out.writeInt (TDBProtocol.RESP_OK);
        out.writeLong (typeVersion);
    }

    private void                doSetStreamType (VSChannel ds) throws IOException {
        final DXTickStream          stream = getStream (ds);
        DataInputStream             in = ds.getDataInputStream();
        boolean                     polymorphic = in.readBoolean ();
        RecordClassSet              md = (RecordClassSet) TDBProtocol.readClassSet (in);

        ((LockVerifier) stream).checkExclusiveWrite(readLock(ds));
        
        if (polymorphic)
            stream.setPolymorphic (md.getTopTypes ());
        else
            stream.setFixedType (md.getTopType (0));
        
        out.writeInt (TDBProtocol.RESP_OK);
    }

    private void                doGetStream(VSChannel ds) throws IOException {
        String key = ds.getDataInputStream().readUTF();
        DXTickStream stream = db.getStream(key);

        // writing stream may produce exceptions, so
        buffer.reset();
        if (stream != null)
            TDBProtocol.writeStream(dout, stream, clientVersion);

        out.writeInt(TDBProtocol.RESP_OK);
        out.writeBoolean(stream != null);
        if (stream != null)
            buffer.writeTo(out);
    }
    
    private void                doGetStreamDescr (VSChannel ds) throws IOException {
        final TickStream            stream = getStream (ds);
        final String description = stream.getDescription();
        
        out.writeInt (TDBProtocol.RESP_OK);
        TDBProtocol.writeNullableString (description, out);
    }

    private void                doSetStreamDescr (VSChannel ds) throws IOException {
        DXTickStream            stream = getStream (ds);
        final String description = TDBProtocol.readNullableString(ds.getDataInputStream());

        ((LockVerifier) stream).checkExclusiveWrite(readLock(ds));

        stream.setDescription(description);
        out.writeInt (TDBProtocol.RESP_OK);
    }
    
    private void                doGetStreamTNF (VSChannel ds) throws IOException {
        final DXTickStream          stream = getStream (ds);
        final int distributionFactor = stream.getDistributionFactor();
        
        out.writeInt (TDBProtocol.RESP_OK);
        out.writeInt (distributionFactor);
    }
    
    public static RecordClassSet  getMetaData (TickStream stream) {
        // Shortcut
        if (stream instanceof FriendlyStream) 
            return (((FriendlyStream) stream).getMetaData ());

        RecordClassSet  ret = new RecordClassSet ();

        if (stream.isFixedType ())
            ret.addContentClasses (stream.getFixedType ());
        else
            ret.addContentClasses (stream.getPolymorphicDescriptors ());

        return (ret);
    }

    private void                startDownload (VSChannel ds, int clientVersion)
        throws IOException, InterruptedException
    {
        try {
            TimebaseAccessController ac = context != null ? context.ac : null;
            DownloadHandlerFactory.start(user, ds, db, executor, ac, clientVersion, aeronThreadTracker, aeronContext);
        } catch (DownloadHandler.QueryCompilationFailed x) {
            // ignore, already reported to user
            ds.close (true);
        } catch (ChannelClosedException x) {
            // ignore
        }
    }

    private void                startMulticastDownload (VSChannel ds, int clientVersion)
        throws IOException, InterruptedException
    {
        try {
            TimebaseAccessController ac = context != null ? context.ac : null;
            if (ac != null) {
                throw new UnsupportedOperationException();
            }
            DownloadHandlerFactoryMulticast.start(user, ds, db, executor, null, clientVersion, aeronThreadTracker, aeronContext, RequestHandler.isLocal(ds));
        } catch (DownloadHandler.QueryCompilationFailed x) {
            // ignore, already reported to user
            ds.close (true);
        } catch (ChannelClosedException x) {
            // ignore
        }
    }


    static boolean isLocal(@Nonnull VSChannel ds) {
        String remoteAddress = ds.getRemoteAddress();
        return isLocal(remoteAddress);
    }

    @Nullable
    static String getAddress(@Nonnull VSChannel ds) {
        String remoteAddress = ds.getRemoteAddress();
        int startIndex = remoteAddress.indexOf('/');
        int endIndex = remoteAddress.indexOf(':');
        if (startIndex >= 0 && endIndex > startIndex + 1) {
            return remoteAddress.substring(startIndex + 1, endIndex);
        } else {
            return null;
        }
    }

    /**
     * Check if address represents local client.
     *
     * @param remoteAddress Address has format: "[hostname]/ip_address:port". See {@link InetSocketAddress#toString} (for resolved address).
     *
     */
    // TODO: Move this to a library class?
    static boolean isLocal(@Nullable String remoteAddress) {
        if (remoteAddress != null) {
            if (remoteAddress.startsWith("/127.0.0.1:")) {
                // Fast path
                return true;
            }
            int startIndex = remoteAddress.indexOf('/');
            int endIndex = remoteAddress.indexOf(':');
            if (startIndex >= 0 && endIndex > startIndex + 1) {
                String ipAddress = remoteAddress.substring(startIndex + 1, endIndex);
                try {
                    return InetAddress.getByName (ipAddress).isLoopbackAddress ();
                } catch (UnknownHostException x) {
                    LOG.warn("Failed to determine if address '%s' is local.").with(ipAddress);
                }
            }
        }
        return false;
    }

    private void                doUpload(VSChannel ds, int clientVersion) throws IOException {
        try {
//            Double limit = LicenseController.getDbSizeLimit();
//            if (limit != null)
//                LicenseController.validateDbSize(db.getSizeOnDisk());
            UploadHandlerFactory.start(user, db, ds, executor, clientVersion, aeronContext, aeronThreadTracker);
        } catch (ChannelClosedException x) {
            // ignore
        }
    }

    private void                doClearData(VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);
        final IdentityKey [] ids = TDBProtocol.readInstrumentIdentities (ds.getDataInputStream());

        ((LockVerifier) stream).checkExclusiveWrite(readLock(ds));

        stream.clear( ids);
        out.writeInt (TDBProtocol.RESP_OK);
        out.flush();
    }

    private void               doTruncateData(VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);
        DataInputStream din = ds.getDataInputStream();
        long time = din.readLong();
        final IdentityKey [] ids = TDBProtocol.readInstrumentIdentities (din);

        ((LockVerifier) stream).checkExclusiveWrite(readLock(ds));

        stream.truncate(time, ids);
        out.writeInt (TDBProtocol.RESP_OK);
        out.flush();
    }

    private void                doRunTransformation(VSChannel ds)
            throws IOException
    {
        final DXTickStream            stream = getStream (ds);
        TransformationTask task = TDBProtocol.readTransformationTask(ds.getDataInputStream());

        ((LockVerifier) stream).checkExclusiveWrite(readLock(ds));

        stream.execute(task);

        out.writeInt (TDBProtocol.RESP_OK);
    }

    private void                doPurge(VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);
        long time = ds.getDataInputStream().readLong();

        ServerLock lock = readLock(ds); // do not verify lock for the purge
        //stream.verify(readLock(ds), LockType.WRITE);

        stream.purge(time);
        out.writeInt (TDBProtocol.RESP_OK);
    }

    private void                doPurgeSpace(VSChannel ds) throws IOException {
        final DXTickStream stream = getStream(ds);
        DataInputStream dis = ds.getDataInputStream();
        long time = dis.readLong();
        String space = dis.readUTF();


        ServerLock lock = readLock(ds); // do not verify lock for the purge
        //stream.verify(readLock(ds), LockType.WRITE);

        stream.purge(time, space);
        out.writeInt(TDBProtocol.RESP_OK);
    }

    private void                doGetBackgroundProcess(VSChannel ds)  throws IOException  {
        final DXTickStream      stream = getStream (ds);
        BackgroundProcessInfo process = stream.getBackgroundProcess();
        
        out.writeInt (TDBProtocol.RESP_OK);
        TDBProtocol.writeBGProcessInfo(process, out);
    }

    private void                doAbortBackgroundProcess(VSChannel ds) throws IOException {
        final DXTickStream      stream = getStream (ds);
        stream.abortBackgroundProcess();
        
        out.writeInt (TDBProtocol.RESP_OK);
    }

    private void                doLockStream(VSChannel ds) throws IOException {
        final DXTickStream      stream = getStream (ds);        
        LockType type = ds.getDataInputStream().readBoolean() ? LockType.READ : LockType.WRITE;

        ServerLock lock = (ServerLock) stream.lock(type);
        lock.setClientId(ds.getClientId());
        setLockMonitorInfo(lock, ds);
        out.writeInt (TDBProtocol.RESP_OK);
        writeLock(ds.getDataOutputStream(), lock);
    }

    private void                doTryLockStream(VSChannel ds) throws IOException {
        final DXTickStream      stream = getStream (ds);
        LockType type = ds.getDataInputStream().readBoolean() ? LockType.READ : LockType.WRITE;
        long timeout = ds.getDataInputStream().readLong();

        ServerLock lock = (ServerLock) stream.tryLock(type, timeout);
        lock.setClientId(ds.getClientId());
        setLockMonitorInfo(lock, ds);
        out.writeInt (TDBProtocol.RESP_OK);
        writeLock(ds.getDataOutputStream(), lock);
    }

    private void                setLockMonitorInfo(ServerLock lock, VSChannel ds) {
        if (lock instanceof TBLock) {
            TBLock tbLock = (TBLock) lock;
            tbLock.setUser(user != null ? user.getName() : null);
            tbLock.setApplication(ds.getRemoteApplication());
        }
    }

    private void                doUnlockStream(VSChannel ds) throws IOException {
        final DXTickStream      stream = getStream (ds);

        ServerLock lock = readLock(ds);
        if (lock != null) {
            DBLock dbLock = stream.verify(lock, lock.getType());
            dbLock.release();
        } else {
            TickDBServer.LOGGER.severe ("Trying to unlock with empty lock");
        }

        out.writeInt (TDBProtocol.RESP_OK);        
    }

    private void                doGetLockState(VSChannel ds) throws IOException {
        final DXTickStream      stream = getStream (ds);

        try {
            ServerLock lock = readLock(ds);
            if (lock != null) {
                DBLock dbLock = stream.verify(lock, lock.getType());
                out.writeInt (TDBProtocol.RESP_OK);
                out.writeBoolean(dbLock.isValid());
            } else {
                out.writeInt (TDBProtocol.RESP_OK);
                out.writeBoolean(false);
            }

        } catch (StreamLockedException e) {
            out.writeInt (TDBProtocol.RESP_OK);
            out.writeBoolean(false);
        }        
    }

    private void                 doEnableVersioning(VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);

        boolean enableVersioning = stream.enableVersioning();

        out.writeInt (TDBProtocol.RESP_OK);
        out.writeBoolean(enableVersioning);
        out.flush();
    }

    private void                doGetDataVersion(VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);
        final long version = stream.getDataVersion();
        
        out.writeInt (TDBProtocol.RESP_OK);
        out.writeLong(version);
        out.flush();
    }

    private void                doGetReplicaVersion(VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);
        long version = stream.getReplicaVersion();

        out.writeInt (TDBProtocol.RESP_OK);
        out.writeLong(version);
        out.flush();
    }

    private void                doSetReplicaVersion(VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);

        long version = ds.getDataInputStream().readLong();

        stream.setReplicaVersion(version);
        out.writeInt (TDBProtocol.RESP_OK);
        out.flush();
    }

    private void                doGetComposition(VSChannel ds) throws IOException {
        final DXTickStream            stream = getStream (ds);
        final IdentityKey [] ids = TDBProtocol.readInstrumentIdentities (ds.getDataInputStream());
        final IdentityKey[] composition = stream.getComposition(ids);

        out.writeInt (TDBProtocol.RESP_OK);

        TDBProtocol.writeInstrumentIdentities (composition, out);
    }

    private void                doSetStreamOwner(VSChannel ds) throws IOException {
        DXTickStream stream = getStream(ds);
        final String newOwner = TDBProtocol.readNullableString(ds.getDataInputStream());

        ((LockVerifier) stream).checkExclusiveWrite(readLock(ds));

        stream.setOwner(newOwner);
        out.writeInt(TDBProtocol.RESP_OK);
    }

    public static void          writeLock(DataOutputStream dout, DBLockImpl lock)
        throws IOException
    {
        dout.writeBoolean(lock != null);
        if (lock != null) {
            dout.writeUTF(lock.getGuid());
            dout.writeBoolean(lock.getType() == LockType.READ);
        }
    }

    @Nullable
    public static ServerLock     readLock(VSChannel vs) throws IOException {
        return readLock(vs.getDataInputStream(), vs.getClientId());
    }

    public static ServerLock     readLock(DataInputStream din, String clientId) throws IOException {
        boolean exists = din.readBoolean();
        if (exists) {
            String guid = din.readUTF();
            LockType type = din.readBoolean() ? LockType.READ : LockType.WRITE;

            return new ServerLock(type, guid, clientId);
        }

        return null;
    }

    private void doCreateIpcTopic(VSChannel ds, int clientVersion) throws IOException {
        DataInputStream     in = ds.getDataInputStream();

        // Enforce strict version match
        enforceStrictVersionMatch(clientVersion, in);

        CreateTopicRequest request = TopicProtocol.readCreateTopicRequest(in, this.clientVersion);

        String topicKey = request.getTopicKey();
        List<RecordClassDescriptor> types = request.getTypes();
        String copyToStreamKey = request.getTargetStream();
        String copyToSpace = request.getTargetSpace();

        assertTopicCreationAllowed();
        assertTypesNotEmpty(types);
        assertCopyToStreamIsValid(types, copyToStreamKey);

        //String channel = TopicChannelFactory.createIpcChannel();

        CreateTopicResult createTopicResult = topicRegistry.createDirectTopic(topicKey, types, null, aeronContext.getStreamIdGenerator(), TopicType.IPC, null, copyToStreamKey, copyToSpace);

        setupCopyToStream(topicKey, types, copyToStreamKey, createTopicResult, copyToSpace);

        out.writeInt(TDBProtocol.RESP_OK);
        out.flush();

        if (UserLogger.canTrace(user)) {
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "CREATE TOPIC (%s)", topicKey);
        }
    }

    // TODO: Merge with doCreateTopic(...)
    private void                 doCreateMulticastTopic(VSChannel ds, int clientVersion) throws IOException {
        DataInputStream     in = ds.getDataInputStream();

        // Enforce strict version match
        enforceStrictVersionMatch(clientVersion, in);

        CreateMulticastTopicRequest request = TopicProtocol.readCreateMulticastTopicRequest(in, this.clientVersion);

        String topicKey = request.getTopicKey();
        List<RecordClassDescriptor> types = request.getTypes();
        String copyToStreamKey = request.getTargetStream();
        String copyToSpace = request.getTargetSpace();

        assertTopicCreationAllowed();
        assertTypesNotEmpty(types);
        assertCopyToStreamIsValid(types, copyToStreamKey);

        TopicChannelOptionMap channelOptions = new TopicChannelOptionMap();
        channelOptions.put(TopicChannelOption.MULTICAST_ENDPOINT_HOST, request.getEndpointHost());
        channelOptions.put(TopicChannelOption.MULTICAST_ENDPOINT_PORT, request.getEndpointPort());
        channelOptions.put(TopicChannelOption.MULTICAST_NETWORK_INTERFACE, request.getNetworkInterface());
        channelOptions.put(TopicChannelOption.MULTICAST_TTL, request.getTtl());

        CreateTopicResult createTopicResult = topicRegistry.createDirectTopic(topicKey, types, null, aeronContext.getStreamIdGenerator(), TopicType.MULTICAST, channelOptions.getValueMap(), copyToStreamKey, copyToSpace);

        setupCopyToStream(topicKey, types, copyToStreamKey, createTopicResult, copyToSpace);

        out.writeInt(TDBProtocol.RESP_OK);
        out.flush();

        if (UserLogger.canTrace(user)) {
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "CREATE MULTICAST TOPIC (%s)", request.getTopicKey());
        }
    }
    private void                 doCreateCustomTopic(VSChannel ds, int clientVersion) throws IOException {
        DataInputStream     in = ds.getDataInputStream();

        // Enforce strict version match
        enforceStrictVersionMatch(clientVersion, in);

        CreateCustomTopicRequest request = TopicProtocol.readCreateCustomTopicRequest(in, this.clientVersion);

        String topicKey = request.getTopicKey();
        List<RecordClassDescriptor> types = request.getTypes();
        String copyToStreamKey = request.getTargetStream();
        String copyToSpace = request.getTargetSpace();

        assertTopicCreationAllowed();
        assertTypesNotEmpty(types);
        assertCopyToStreamIsValid(types, copyToStreamKey);

        TopicChannelOptionMap channelOptions = new TopicChannelOptionMap();
        fillChannelOptionsFromAttributes(channelOptions, request.getAttributes());

        // Validate
        TopicType topicType = request.getTopicType();
        if (topicType == TopicType.UDP_SINGLE_PUBLISHER) {
            if (!channelOptions.hasValue(TopicChannelOption.PUBLISHER_HOST)) {
                throw new IllegalArgumentException("SINGLE_PUBLISHER topic type requires publisher host to be set");
            }
        }


        CreateTopicResult createTopicResult = topicRegistry.createDirectTopic(topicKey, types, null, aeronContext.getStreamIdGenerator(), topicType, channelOptions.getValueMap(), copyToStreamKey, copyToSpace);

        setupCopyToStream(topicKey, types, copyToStreamKey, createTopicResult, copyToSpace);

        out.writeInt(TDBProtocol.RESP_OK);
        out.flush();

        if (UserLogger.canTrace(user)) {
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "CREATE CUSTOM %s TOPIC (%s)", topicType, request.getTopicKey());
        }
    }

    private void assertTopicCreationAllowed() {
        if (!aeronContext.isAeronEnabled()) {
            // While we do not directly require Aeron to be enabled on the server side when we create topic.
            // However, zero limit means that support of topics is generally not configured.
            if (aeronContext.getTopicTotalTermBufferLimit() == 0) {
                throw new IllegalStateException("Aeron is not enabled on TimeBase server");
            }
        }
    }

    private void assertCopyToStreamIsValid(List<RecordClassDescriptor> types, @Nullable String copyToStreamKey) {
        CopyTopicToStreamTaskManager.preValidateCopyToStreamKey(db, types, copyToStreamKey);
        if (copyToStreamKey != null) {
            // We will need to start copyToStream process for that topic.
            // This means that TimeBase will consume data from topic.
            // So we have to ensure that Aeron is actually available before we start to create the topic.
            if (!aeronContext.isAeronEnabled()) {
                throw new IllegalStateException("copyToStreamKey is set but Aeron is not enabled on TimeBase server");
            }
            assertAeronClientIsValid();
        }
    }

    private void assertAeronClientIsValid() {
        try {
            Aeron aeron = aeronContext.getAeron();
            assert aeron != null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Aeron client", e);
        }
    }

    private static void fillChannelOptionsFromAttributes(TopicChannelOptionMap channelOptions, Map<CreateCustomTopicRequest.Field, String> attributes) {
        // TODO: Process other options
        String publisherAddress = attributes.get(CreateCustomTopicRequest.Field.PUBLISHER_ADDRESS);
        if (publisherAddress != null) {
            String[] parts = publisherAddress.split(":", 2);
            channelOptions.put(TopicChannelOption.PUBLISHER_HOST, parts[0]);
            if (parts.length == 2) {
                channelOptions.put(TopicChannelOption.PUBLISHER_PORT, parts[1]);
            }
        }
        String termBufferLength = attributes.get(CreateCustomTopicRequest.Field.TERM_BUFFER_LENGTH);
        if (termBufferLength != null) {
            channelOptions.put(TopicChannelOption.TERM_BUFFER_LENGTH, termBufferLength);
        }
    }

    private static void assertTypesNotEmpty(List<RecordClassDescriptor> types) {
        if (types.isEmpty()) {
            throw new IllegalArgumentException("Type set can't be empty");
        }
    }

    private void setupCopyToStream(String topicKey, List<RecordClassDescriptor> types, String copyToStreamKey, CreateTopicResult createTopicResult, @Nullable String targetSpace) {
        if (copyToStreamKey != null) {
            CopyTopicToStreamTaskManager copyTopicToStream = new CopyTopicToStreamTaskManager(db, aeronContext, aeronThreadTracker, topicRegistry);
            copyTopicToStream.subscribeToStreamCopyOrRollback(topicKey, types, copyToStreamKey, targetSpace, createTopicResult);
        }
    }

    private void                doDeleteTopic (VSChannel ds, int clientVersion) throws IOException {
        DataInputStream in = ds.getDataInputStream();

        readProtocolVersionWithoutCheck(clientVersion, in);

        DeleteTopicRequest request = TopicProtocol.readDeleteTopicRequest(in);
        String topicKey = request.getTopicKey();
        topicRegistry.deleteDirectTopic(topicKey);

        out.writeInt(TDBProtocol.RESP_OK);
        out.flush();

        if (UserLogger.canTrace(user)) {
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "DELETE TOPIC (%s)", topicKey);
        }
    }

    /**
     * Implementation note:
     *  For this method we try to provide backward compatibility with older clients because
     *  clients are likely to call that method as part of listChannels() call even if they do not directly use topics.
     */
    private void                doListTopics (VSChannel ds, int clientVersion) throws IOException {
        DataInputStream in = ds.getDataInputStream();

        readProtocolVersionWithoutCheck(clientVersion, in);

        List<String> topics = topicRegistry.listDirectTopics();

        out.writeInt(TDBProtocol.RESP_OK);
        TopicProtocol.writeListTopicsResponse(out, new ListTopicsResponse(topics));
        out.flush();

        if (UserLogger.canTrace(user)) {
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "LIST TOPICS");
        }
    }

    /**
     * Implementation note:
     *  For this method we try to provide backward compatibility with older clients because
     *  clients are likely to call that method as part of listChannels() call even if they do not directly use topics.
     */
    private void                doGetTopicMetadata (VSChannel ds, int clientVersion) throws IOException {
        DataInputStream in = ds.getDataInputStream();

        readProtocolVersionWithoutCheck(clientVersion, in);

        GetTopicMetadataRequest request = TopicProtocol.readGetTopicMetadataRequest(in);
        String topicKey = request.getTopicKey();

        ImmutableList<RecordClassDescriptor> topicTypes = topicRegistry.getTopicTypes(topicKey);

        out.writeInt(TDBProtocol.RESP_OK);
        TopicProtocol.writeGetTopicMetadataResponse(out, new GetTopicMetadataResponse(topicTypes));
        out.flush();

        if (UserLogger.canTrace(user)) {
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "GET TOPIC METADATA");
        }
    }

    /**
     * Reads topic sub protocol version but does not enforce it.
     *
     * <p>This method should be only used for cases when backwards compatibility is supported.
     */
    private static void readProtocolVersionWithoutCheck(int clientVersion, DataInputStream in) throws IOException {
        // Older client version are compatible for now and does not need special handling for this method yet
        if (clientVersion >= TDBProtocol.TOPIC_SUB_PROTOCOL_VERSION) {
            int clientTopicProtocolVersion = TopicProtocol.readTopicProtocolVersion(in);
            LOG.debug("Client topic protocol version: %s").with(clientTopicProtocolVersion);
            if (clientTopicProtocolVersion <= 2) {
                throw new IllegalArgumentException("Client and server topic protocol version mismatch: client=" + clientTopicProtocolVersion + ", server=" + DirectProtocol.PROTOCOL_VERSION);
            }
        }
    }

    /**
     * Reads topic sub protocol version from client and ensures that this version is exactly version provided by server.
     *
     * <p>Should be used in all cases where exact match of versions is required.
     */
    private static void enforceStrictVersionMatch(int clientVersion, DataInputStream in) throws IOException {
        if (clientVersion >= TDBProtocol.TOPIC_SUB_PROTOCOL_VERSION) {
            ensureSameTopicProtocolVersion(in);
        } else {
            throw makeIncompatibleClientVersionErrorForTopics();
        }
    }


    private void doCreateTopicPublisher(VSChannel ds, int clientVersion) throws IOException {
        DataInputStream in = ds.getDataInputStream();

        enforceStrictVersionMatch(clientVersion, in);

        AddTopicPublisherRequest request = TopicProtocol.readAddTopicPublisherRequest(in);

        try {
            if (!aeronContext.isAeronEnabled()) {
                throw new IllegalStateException("Aeron support is disabled on the server");
            }

            boolean local = isLocal(ds);
            LoaderSubscriptionResult result = topicRegistry.addLoader(request.getTopicKey(), local, aeronContext.getPublicAddress());

            // TODO: We are keeping this only to be able to handle loader disconnects
            ds.setAvailabilityListener(() -> {
                try {
                    DataInputStream dis = ds.getDataInputStream();
                    int available;
                    while ((available = dis.available()) > 0) {
                        dis.skipBytes(available);
                    }
                } catch (EOFException eoq) {
                    // Graceful close
                    LOG.info("Client closed topic publisher connection: remoteAddress=%s clientId=%s")
                            .with(ds.getRemoteAddress()).with(ds.getClientId());
                } catch (IOException e) {
                    LOG.warn("Client closed topic publisher connection unexpectedly (remoteAddress=%s clientId=%s): %s")
                            .with(ds.getRemoteAddress()).with(ds.getClientId()).with(e);
                }
            });

            assert local || result.getTopicType() != TopicType.IPC;
            TopicTransferType transferType = result.getTopicType() == TopicType.IPC ? TopicTransferType.IPC : TopicTransferType.UDP;
            // Don't send Aeron driver directory path to remote client
            String aeronDir = local ? aeronContext.getAeronDir() : null;

            out.writeInt(TDBProtocol.RESP_OK);
            // TODO: IMPORTANT: Send metadata channel to client
            LOG.debug("Sending topic publisher channel to client: dataStreamId=%s, channel=%s").with(result.getDataStreamId()).with(result.getPublisherChannel());
            TopicProtocol.writeAddTopicPublisherResponse(out, new AddTopicPublisherResponse(
                    transferType,
                    result.getTypes(),
                    result.getPublisherChannel(),
                    aeronDir,
                    result.getDataStreamId()
            ), this.clientVersion);
            out.flush();

            if (UserLogger.canTrace(user)) {
                UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "CREATE TOPIC PUBLISHER (%s)", request.getTopicKey());
            }
        } catch (IllegalArgumentException e) {
            out.writeInt(TDBProtocol.RESP_ERROR);
            TDBProtocol.writeError(out, e);
            out.flush();
            ds.close();
        }
    }
    /**
     */
    private void doCreateTopicSubscriber(VSChannel ds, int clientVersion) throws IOException {
        DataInputStream in = ds.getDataInputStream();

        // Enforce strict version match
        enforceStrictVersionMatch(clientVersion, in);

        AddTopicSubscriberRequest request = TopicProtocol.readAddTopicSubscriberRequest(in);
        boolean local = isLocal(ds);
        String remoteClientAddress = getAddress(ds);
        ReaderSubscriptionResult result = topicRegistry.addReader(request.getTopicKey(), local, aeronContext.getPublicAddress(), remoteClientAddress);

        assert local || result.getTopicType() != TopicType.IPC;
        TopicTransferType transferType = result.getTopicType() == TopicType.IPC ? TopicTransferType.IPC : TopicTransferType.UDP;
        // Don't send Aeron driver directory path to remote client
        String aeronDir = local ? aeronContext.getAeronDir() : null;

        out.writeInt(TDBProtocol.RESP_OK);
        LOG.debug("Sending topic subscriber channel to client: dataStreamId=%s, channel=%s").with(result.getDataStreamId()).with(result.getSubscriberChannel());
        TopicProtocol.writeAddTopicSubscriberResponse(out, new AddTopicSubscriberResponse(
                transferType,
                result.getTypes(),
                result.getSubscriberChannel(),
                aeronDir,
                result.getDataStreamId()
        ));
        out.flush();

        if (UserLogger.canTrace(user)) {
            UserLogger.trace(user, ds.getRemoteAddress(), ds.getRemoteApplication(), "CREATE TOPIC SUBSCRIBER (%s)", request.getTopicKey());
        }
    }

    private static void ensureSameTopicProtocolVersion(DataInputStream in) throws IOException {
        int clientTopicProtocolVersion = TopicProtocol.readTopicProtocolVersion(in);
        if (clientTopicProtocolVersion != DirectProtocol.PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Client and server topic protocol version mismatch: client="
                    + clientTopicProtocolVersion + ", server=" + DirectProtocol.PROTOCOL_VERSION
                    + ". Recommended TimeBase client version: " + Version.getVersion());
        }
    }

    @NotNull
    private static IllegalArgumentException makeIncompatibleClientVersionErrorForTopics() {
        return new IllegalArgumentException("Client does not support this version of topic protocol. Please upgrade client."
                + " Recommended TimeBase client version: " + Version.getVersion());
    }
}