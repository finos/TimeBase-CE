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
package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.pub.md.json.SchemaBuilder;
import com.epam.deltix.qsrv.hf.pub.md.json.SchemaDef;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.*;
import com.epam.deltix.qsrv.hf.tickdb.impl.ServerLock;
import com.epam.deltix.qsrv.hf.tickdb.impl.ServerStreamWrapper;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamImpl;
import com.epam.deltix.qsrv.hf.tickdb.impl.lock.ServerLockOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.TBObject;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.StreamChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.time.Periodicity;
import org.owasp.encoder.Encode;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;

import static com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol.*;
import static com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol.marshallUHF;
import static com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler.sendError;

public final class StreamHandler {

    static void     createStream(DXTickDB db, CreateStreamRequest request, HttpServletResponse response) throws IOException {
        StreamDef def = request.options;

        StreamOptions options = def.convert();

        String metadataJson = request.options.metadataJson;
        if (!StringUtils.isEmpty(metadataJson)) {
            SchemaDef schema = HTTPProtocol.JSON_MAPPER.readValue(metadataJson, SchemaDef.class);
            options.setMetaData(def.polymorphic, SchemaBuilder.toClassSet(schema));
        } else {
            String metadata = request.options.metadata;
            if (!StringUtils.isEmpty(metadata))
                options.setMetaData(def.polymorphic, (RecordClassSet) unmarshallUHF(new StringReader(metadata)));
        }

        db.createStream(request.key, options);

        response.setStatus(HttpServletResponse.SC_OK);
    }

//    static void     createStream(DXTickDB db, CreateFileStreamRequest request, HttpServletResponse response) throws IOException {
//        db.createFileStream(request.key, request.dataFile);
//
//        response.setStatus(HttpServletResponse.SC_OK);
//    }

    static void processGetRange(DXTickDB db, GetRangeRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            final long[] range = req.identities != null ?
                    stream.getTimeRange(identityKeys(req.identities)) : stream.getTimeRange();

            final GetRangeResponse r = new GetRangeResponse();
            if (range != null)
                r.timeRange = new TimeRange(range[0], range[1]);

            marshall(r, response.getOutputStream());
        }
    }

    static void processGetPeriodicity(DXTickDB db, GetPeriodicityRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            final GetPeriodicityResponse r = new GetPeriodicityResponse();
            r.periodicity = stream.getPeriodicity();

            marshall(r, response.getOutputStream());
        }
    }

    static void processLock(DXTickDB db, LockStreamRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        LockStreamResponse r = new LockStreamResponse();
        ServerLock lock;

        try {
            LockOptions options = req.write ?
                ServerLockOptions.createWrite(req.startTime, req.endTime, req.sid) :
                ServerLockOptions.create(LockType.READ, req.sid);
            if (req.timeout > 0)
                lock = (ServerLock) stream.tryLock(options, req.timeout);
            else
                lock = (ServerLock) stream.lock(options);

            if (lock instanceof TBObject) {
                ((TBObject) lock).setApplication(req.applicationName);
            }

            r.id = lock.getGuid();
            r.write = req.write;

            marshall(r, response.getOutputStream());

        } catch (StreamLockedException | UnsupportedOperationException e) {
            sendError(response, e);
        }
    }

    static void processUnlock(DXTickDB db, UnlockStreamRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream == null)
            return;

        LockOptions options = req.write ?
            WriteLockOptions.create(req.startTime, req.endTime) : LockOptions.create(LockType.READ);

        ServerLock lock = new ServerLock(options, req.id, null);
        try {
            DBLock dbLock = stream.verify(lock, lock.getType());
            dbLock.release();
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (StreamLockedException | UnsupportedOperationException e) {
            sendError(response, e);
        }
    }

    static void processSetSchema(DXTickDB db, SetSchemaRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            verifyWrite(stream, req.token);

            RecordClassSet classSet = (RecordClassSet) unmarshallUHF(new StringReader(req.schema));

            if (req.polymorphic)
                stream.setPolymorphic(classSet.getTopTypes());
            else
                stream.setFixedType(classSet.getTopType(0));
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }

    static void     processChangeSchema(DXTickDB db, ChangeSchemaRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            verifyWrite(stream, req.token);

            RecordClassSet classSet;
            MetaDataChange.ContentType outType = req.polymorphic ? MetaDataChange.ContentType.Polymorphic : MetaDataChange.ContentType.Fixed;
            String metadataJson = req.schemaJson;
            if (!StringUtils.isEmpty(metadataJson)) {
                SchemaDef schema = HTTPProtocol.JSON_MAPPER.readValue(metadataJson, SchemaDef.class);
                outType = schema.types.length > 1 ? MetaDataChange.ContentType.Polymorphic : MetaDataChange.ContentType.Fixed;
                classSet = SchemaBuilder.toClassSet(schema);
            } else {
                classSet = (RecordClassSet) unmarshallUHF(new StringReader(req.schema));
            }

            RecordClassSet source = new RecordClassSet ();
            MetaDataChange.ContentType  inType;

            if (stream.isFixedType ()) {
                inType = MetaDataChange.ContentType.Fixed;
                source.addContentClasses (stream.getFixedType ());
            } else {
                inType = MetaDataChange.ContentType.Polymorphic;
                source.addContentClasses (stream.getPolymorphicDescriptors ());
            }

            SchemaMapping mapping = new SchemaMapping();

            // process mapping for descriptors and fields

            if (req.mappings != null) {
                for (Map.Entry<String, String> entry : req.mappings.entrySet()) {
                    String[] from = entry.getKey().split(":");

                    if (from.length == 2) { // DataField
                        DataField fromField = source.findField(from[0], from[1]);
                        String[] to = entry.getValue().split(":");
                        DataField toField = classSet.findField(to[0], to[1]);

                        mapping.fields.put(fromField, toField);

                    } else if (from.length == 1) { // CD
                        mapping.descriptors.put(entry.getKey(), entry.getValue());
                    } else {
                        throw new IllegalStateException("Unknown name: " + entry.getKey());
                    }
                }
            }

            StreamMetaDataChange changes = new SchemaAnalyzer(mapping).getChanges(source, inType, classSet, outType);

            if (req.defaults != null) {
                for (Map.Entry<String, String> entry : req.defaults.entrySet()) {
                    String[] from = entry.getKey().split(":");

                    if (from.length == 2) { // DataField
                        RecordClassDescriptor cd = getClassDescriptorByName(changes.getTarget(), from[0]);

                        ClassDescriptorChange change = changes.getChange(null, cd);
                        if (change != null && cd != null) {
                            AbstractFieldChange[] fieldChanges = change.getFieldChanges(null, cd.getField(from[1]));

                            for (AbstractFieldChange c : fieldChanges) {

                                String fullName = cd.getName() + " [" + c.getTarget().getName() + "]";

                                if (c.hasErrors()) {
                                    String value = entry.getValue();

                                    if (c instanceof FieldTypeChange) {
                                        ((FieldTypeChange)c).setDefaultValue(value);
                                        if (c.hasErrors())
                                            throw new IllegalStateException(fullName + ": default value expected.");
                                    } else if (c instanceof CreateFieldChange) {
                                        ((CreateFieldChange)c).setInitialValue(value);
                                        if (c.hasErrors())
                                            throw new IllegalStateException(fullName + ": default value expected.");
                                    } else if (c instanceof FieldModifierChange) {
                                        if (c.getTarget() instanceof StaticDataField)
                                            ((FieldModifierChange)c).setInitialValue(((StaticDataField)c.getTarget()).getStaticValue());
                                        else
                                            ((FieldModifierChange)c).setInitialValue(value);

                                        if (c.hasErrors())
                                            throw new IllegalStateException(fullName + ": default value expected.");
                                    }
                                }
                            }

                        }

                    } else {
                        throw new IllegalStateException("Unknown field reference: " + entry.getKey());
                    }
                }
            }

            SchemaChangeTask task = new SchemaChangeTask(changes);
            task.setBackground(req.background);

            stream.execute(task);
        }

        response.setStatus(HttpServletResponse.SC_OK);
    }

    static DataField findField(ClassSet<RecordClassDescriptor> set, String typeName, String fieldName) {

        ClassDescriptor[] classes = set.getClasses();
        for (int i = 0; i < classes.length; i++) {
            if (typeName.equals(classes[i].getName())) {
                return ((RecordClassDescriptor) classes[i]).getField(fieldName);
            }
        }

        return null;
    }

    static RecordClassDescriptor getClassDescriptorByName(ClassSet<RecordClassDescriptor> set, String name) {
        ClassDescriptor[] classes = set.getClasses();
        for (int i = 0; i < classes.length; i++) {
            if (name.equals(classes[i].getName()))
                return (RecordClassDescriptor) classes[i];
        }

        return null;
    }

    static void processGetSchema(DXTickDB db, GetSchemaRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            final RecordClassSet rcs = stream.getStreamOptions().getMetaData();
            marshallUHF(rcs, response.getOutputStream());
        }
    }

    static void processListEntities(DXTickDB db, ListEntitiesRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            final ListEntitiesResponse r = new ListEntitiesResponse();
            r.identities = stream.listEntities();

            marshall(r, response.getOutputStream());
        }
    }

    static void processListStreams(DXTickDB db, HttpServletResponse response) throws IOException {
        final ArrayList<String> streams = new ArrayList<>();
        for (DXTickStream stream : db.listStreams()) {
            streams.add(stream.getKey());
        }

        final ListStreamsResponse r = new ListStreamsResponse();
        r.streams = streams.toArray(new String[streams.size()]);

        marshall(r, response.getOutputStream());
    }

    public static void processLoadStreams(DXTickDB db, HttpServletResponse resp) throws IOException, JAXBException {

        final ArrayList<String> streams = new ArrayList<>();
        final ArrayList<StreamDef> options = new ArrayList<>();

        StringWriter writer = new StringWriter();

        for (DXTickStream stream : db.listStreams()) {
            String key = stream.getKey();
            streams.add(key);

            StreamDef streamDef = new StreamDef(stream.getStreamOptions());

            writer.getBuffer().setLength(0);
            marshallUHF(stream.getStreamOptions().getMetaData(), writer);
            streamDef.metadata = writer.getBuffer().toString();
            streamDef.metadataJson = HTTPProtocol.JSON_MAPPER.writeValueAsString(
                SchemaBuilder.toSchemaDef(
                    stream.getStreamOptions().getMetaData(), false
                )
            );

            options.add(streamDef);
        }

        final LoadStreamsResponse r = new LoadStreamsResponse();
        r.streams = streams.toArray(new String[streams.size()]);
        r.options = options.toArray(new StreamDef[options.size()]);

        marshall(r, resp.getOutputStream());
    }

    static void processClear(DXTickDB db, ClearRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        if (stream != null) {
            verifyWrite(stream, req.token);

            if (req.identities == null || req.identities.length == 0)
                stream.clear();
            else
                stream.clear(identityKeys(req.identities));

            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    static void processPurge(DXTickDB db, PurgeRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            stream.purge(req.time);
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    static void processTruncate(DXTickDB db, TruncateRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            verifyWriteRange(stream, req.token, req.time, Long.MAX_VALUE);

            if (req.identities != null && req.identities.length > 0)
                stream.truncate(req.time, identityKeys(req.identities));
            else
                stream.truncate(req.time);

            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    static void processDeleteData(DXTickDB db, DeleteDataRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
//            verifyLock(stream, req.token, LockType.WRITE); todo: this check is commented for RequestHandler

            IdentityKey[] instruments = identityKeys(req.symbols);
            if (instruments != null && instruments.length > 0) {
                stream.delete(TimeStamp.fromNanoseconds(req.fromNs), TimeStamp.fromNanoseconds(req.toNs), instruments);
            } else {
                stream.delete(TimeStamp.fromNanoseconds(req.fromNs), TimeStamp.fromNanoseconds(req.toNs));
            }

            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    static void processDelete(DXTickDB db, DeleteRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        if (stream != null) {
            verifyWrite(stream, req.token);

            stream.delete();
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    static void processAbortBG(DXTickDB db, AbortBGProcessRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        if (stream != null) {
            stream.abortBackgroundProcess();
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    static void processGetBG(DXTickDB db, GetBGProcessRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        if (stream != null) {
            BackgroundProcessInfo process = stream.getBackgroundProcess();

            GetBGProcessResponse r = new GetBGProcessResponse(process);
            marshall(r, response.getOutputStream());

            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    public static DXTickStream       getStream(DXTickDB db, StreamRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = db.getStream(req.stream);
        if (stream == null)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Encode.forHtml("deltix.qsrv.hf.tickdb.comm.UnknownStreamException:Stream not found: " + req.stream));

        return stream;
    }

    public static void processRename(DXTickDB db, RenameStreamRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                stream.rename(req.key);
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    public static void setName(DXTickDB db, SetNameRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                stream.setName(req.name);
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    public static void setDescription(DXTickDB db, SetDescriptionRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                stream.setDescription(req.description);
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    public static void setOwner(DXTickDB db, SetOwnerRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                stream.setOwner(req.owner);
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    public static void setDistributionFactor(DXTickDB db, SetDistributionFactorRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                if (stream.getScope() == StreamScope.DURABLE && stream.getFormatVersion() == 4) {
                    StreamChangeTask task = makeStreamChangeTask(stream);
                    task.df = req.distributionFactor;
                    stream.execute(task);
                }
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    public static void setHighAvailability(DXTickDB db, SetHighAvailabilityRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                stream.setHighAvailability(req.highAvailability);
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    public static void setReplicaVersion(DXTickDB db, SetReplicaVersionRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                stream.setReplicaVersion(req.replicaVersion);
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    public static void setPeriodicity(DXTickDB db, SetPeriodicityRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                stream.setPeriodicity(Periodicity.parse(req.periodicity));
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    public static void setBufferOptions(DXTickDB db, SetBufferOptionsRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                if (stream.getScope() == StreamScope.TRANSIENT) {
                    StreamChangeTask task = makeStreamChangeTask(stream);
                    if (task.bufferOptions == null) {
                        task.bufferOptions = new BufferOptions();
                    }
                    task.bufferOptions.initialBufferSize = req.initialBufferSize;
                    task.bufferOptions.maxBufferSize = req.maxBufferSize;
                    task.bufferOptions.maxBufferTimeDepth = req.maxBufferTimeDepth;
                    task.bufferOptions.lossless = req.lossless;
                    stream.execute(task);
                }
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    public static void enableVersioning(DXTickDB db, EnableVersioningRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                stream.enableVersioning();
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    private static StreamChangeTask makeStreamChangeTask(DXTickStream stream) {
        StreamChangeTask task = new StreamChangeTask();
        task.bufferOptions = stream.getStreamOptions().bufferOptions;
        task.df = stream.getDistributionFactor();
        task.name = stream.getName();
        task.description = stream.getDescription();
        task.ha = stream.getHighAvailability();
        task.periodicity = stream.getPeriodicity();
        task.background = false;
        task.change = SchemaAnalyzer.getChanges(stream, stream);

        return task;
    }

    static void processListSpaces(DXTickDB db, ListSpacesRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            final ListSpacesResponse r = new ListSpacesResponse();
            r.spaces = stream.listSpaces();

            marshall(r, response.getOutputStream());
        }
    }

    public static void processRenameSpace(DXTickDB db, RenameSpaceRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null) {
                verifyWrite(stream, req.token);
                stream.renameSpace(req.newName, req.oldName);
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    public static void processDeleteSpaces(DXTickDB db, DeleteSpacesRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        try {
            if (stream != null && req.spaces != null && req.spaces.length > 0)
                stream.deleteSpaces(req.spaces);
        } catch (Exception e) {
            sendError(response, e);
        }
    }

    static void processGetSpaceRange(DXTickDB db, GetSpaceTimeRangeRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            final long[] range = stream.getTimeRange(req.space);

            final GetRangeResponse r = new GetRangeResponse();
            if (range != null)
                r.timeRange = new TimeRange(range[0], range[1]);

            marshall(r, response.getOutputStream());
        }
    }

    static void processDescribeStreamRequest(DXTickDB db, DescribeStreamRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);

        if (stream != null) {
            final DescribeStreamResponse r = new DescribeStreamResponse(stream.describe());
            marshall(r, response.getOutputStream());
        }
    }

    static IdentityKey[] identityKeys(String[] identities) {
        if (identities == null) {
            return null;
        }

        IdentityKey[] keys = new IdentityKey[identities.length];
        for (int i = 0; i < identities.length; ++i) {
            keys[i] = new ConstantIdentityKey(identities[i]);
        }

        return keys;
    }

    public static void verifyWrite(DXTickStream stream, String lockId) {
        TickStreamImpl serverStream = getServerStream(stream);
        if (serverStream != null) {
            serverStream.verifyWrite(lockId);
        }
    }

    public static void verifySharedWrite(DXTickStream stream, String lockId) {
        TickStreamImpl serverStream = getServerStream(stream);
        if (serverStream != null) {
            serverStream.verifySharedWrite(lockId);
        }
    }

    public static void verifyWriteRange(DXTickStream stream, String lockId, long startTime, long endTime) {
        TickStreamImpl serverStream = getServerStream(stream);
        if (serverStream != null) {
            serverStream.verifyWriteRange(lockId, startTime, endTime);
        }
    }

    public static TickStreamImpl getServerStream(DXTickStream stream) {
        if (stream instanceof ServerStreamWrapper) {
            stream = ((ServerStreamWrapper) stream).getNestedInstance();
        }

        if (stream instanceof TickStreamImpl) {
            return (TickStreamImpl) stream;
        }

        return null;
    }

//        IdentityKey[] keys = new IdentityKey[identities.length];
//        for (int i = 0; i < identities.length; ++i) {
//            keys[i] = new ConstantIdentityKey(identities[i]);
//        }
//
//        return keys;
//    }
}