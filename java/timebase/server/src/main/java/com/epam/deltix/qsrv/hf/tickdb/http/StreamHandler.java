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
package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.*;
import com.epam.deltix.qsrv.hf.tickdb.impl.ServerLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.BackgroundProcessInfo;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.lang.StringUtils;
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

        String metadata = request.options.metadata;

        StreamOptions options = def.convert();
        if (!StringUtils.isEmpty(metadata))
            options.setMetaData(def.polymorphic, (RecordClassSet)unmarshallUHF(new StringReader(metadata)));

        db.createStream(request.key, options);

        response.setStatus(HttpServletResponse.SC_OK);
    }

    static void     createStream(DXTickDB db, CreateFileStreamRequest request, HttpServletResponse response) throws IOException {
        db.createFileStream(request.key, request.dataFile);

        response.setStatus(HttpServletResponse.SC_OK);
    }

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
            if (req.timeout > 0)
                lock = (ServerLock) stream.tryLock(req.write ? LockType.WRITE : LockType.READ, req.timeout);
            else
                lock = (ServerLock) stream.lock(req.write ? LockType.WRITE : LockType.READ);

            lock.setClientId(req.sid);

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

        ServerLock lock = new ServerLock(req.write ? LockType.WRITE : LockType.READ, req.id, null);

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
            RecordClassSet classSet = (RecordClassSet) unmarshallUHF(new StringReader(req.schema));

            RecordClassSet source = new RecordClassSet ();
            MetaDataChange.ContentType  inType;

            if (stream.isFixedType ()) {
                inType = MetaDataChange.ContentType.Fixed;
                source.addContentClasses (stream.getFixedType ());
            } else {
                inType = MetaDataChange.ContentType.Polymorphic;
                source.addContentClasses (stream.getPolymorphicDescriptors ());
            }

            MetaDataChange.ContentType  outType = req.polymorphic ? MetaDataChange.ContentType.Polymorphic : MetaDataChange.ContentType.Fixed;

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
                        RecordClassDescriptor cd = MetaDataChange.getClassDescriptor(source, from[0]);

                        ClassDescriptorChange change = changes.getChange(cd, null);
                        if (change != null && cd != null) {
                            AbstractFieldChange[] fieldChanges = change.getFieldChanges(cd.getField(from[1]), null);

                            for (AbstractFieldChange c : fieldChanges) {

                                String fullName = cd.getName() + " [" + c.getSource().getName() + "]";

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
            if (req.identities == null || req.identities.length == 0)
                stream.clear();
            else {
                stream.clear(identityKeys(req.identities));
            }

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
            if (req.identities != null && req.identities.length > 0) {
                stream.truncate(req.time, identityKeys(req.identities));
            } else {
                stream.truncate(req.time);
            }

            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    static void processDelete(DXTickDB db, DeleteRequest req, HttpServletResponse response) throws IOException {
        DXTickStream stream = getStream(db, req, response);
        if (stream != null) {
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
            if (stream != null)
                stream.rename(req.key);
        } catch (Exception e) {
            sendError(response, e);
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
}
