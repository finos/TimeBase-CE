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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.comm.server.MessageCodec;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.SimpleRawDecoder;
import com.epam.deltix.qsrv.hf.tickdb.schema.migration.SchemaChangeMessageBuilder;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.queue.QueueMessageReader;
import com.epam.deltix.qsrv.hf.tickdb.impl.queue.TransientMessageQueue;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.StreamChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.MetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.timebase.messages.schema.SchemaChangeMessage;
import com.epam.deltix.util.concurrent.QuickExecutor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement (name = "queue")
public final class TransientStreamImpl
    extends TickStreamImpl
    implements SingleChannelStream
{
    @XmlElement (name = "bufferOptions")
    private final BufferOptions         bufferOptions;
    private TransientMessageQueue       queue;
    private final QuickExecutor         executor;

    private final SchemaChangeMessageBuilder schemaChangeMessageBuilder = new SchemaChangeMessageBuilder();

    @SuppressWarnings("unused")
    private TransientStreamImpl () {
        // Used by JAXB
        bufferOptions = null;
        executor = null;
    }

    @Override
    public QuickExecutor    getQuickExecutor() {
        return executor != null ? executor : getDBImpl().getQuickExecutor();
    }

    public TransientStreamImpl (StreamOptions options, QuickExecutor executor) {
        super (null, null, options);

        this.executor = executor;
        bufferOptions = options.bufferOptions == null ?
                        new BufferOptions () :
                        options.bufferOptions;
    }

    public TransientStreamImpl (
        DXTickDB            db,
        String              key,
        StreamOptions       options
    )
    {
        super (db, key, options);

        this.executor = getDBImpl().getQuickExecutor();
        bufferOptions = options.bufferOptions == null ?
                new BufferOptions () :
                options.bufferOptions;
    }

    @Override
    public void addInstrument(IdentityKey id) {
        // do nothing
    }

    //
    //  TickStreamImpl implementation
    //
    @Override
    protected void                  onOpen (boolean verify) {
        queue = MessageQueue.forStream (this);
    }

    @Override
    public synchronized void            execute(TransformationTask task) {
        if (task instanceof StreamChangeTask) {
            StreamChangeTask changeTask = (StreamChangeTask) task;
            setName(changeTask.name);
            setDescription(changeTask.description);
            setPeriodicity(changeTask.periodicity);
            setHighAvailability(changeTask.ha);

            if (changeTask.bufferOptions != null) {
                bufferOptions.lossless = changeTask.bufferOptions.lossless;
                bufferOptions.maxBufferSize = changeTask.bufferOptions.maxBufferSize = bufferOptions.maxBufferSize;
                bufferOptions.initialBufferSize = changeTask.bufferOptions.initialBufferSize;
                bufferOptions.maxBufferTimeDepth = changeTask.bufferOptions.maxBufferTimeDepth;
            }
        }

        if (task instanceof SchemaChangeTask) {
            StreamMetaDataChange change = ((SchemaChangeTask) task).change;
            SchemaChangeMessage migrationMessage = schemaChangeMessageBuilder.build(change);
            setMetaData(change.targetType == MetaDataChange.ContentType.Polymorphic, change.getMetaData());

            onSchemaChanged(false, Long.MIN_VALUE);
            onSchemaChanged(migrationMessage);
            
            // re-create queue with new schema
            queue.close();
            onOpen(false);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public int                      getDistributionFactor () {
        return (1);
    }

    public TickReader               createSource (
        long                            time,
        SelectionOptions                options,
        QuickMessageFilter              filter
    )
    {
        QueueMessageReader rawReader = options.live ? queue.getMessageReader(filter, isPolymorphic (), options.realTimeNotification) : new EmptyReader<>(this);

        return (new TickReader (null, MessageCodec.createDecoder(getTypes(), options), rawReader, time, options));
    }

    @Override
    public IdentityKey[] listEntities() {
        return new IdentityKey[0];
    }

    @Override
    public InstrumentMessageSource createSource(long time, SelectionOptions options) {
        return new SingleChannelSource(this, options, time, null, null);
    }

    @Override
    public InstrumentMessageSource createSource(long time, SelectionOptions options, IdentityKey[] identities, String[] types) {
        return new SingleChannelSource(this, options, time, identities, types);
    }

    @Override
    protected void                  onDelete() {
        queue.close();
    }

    @Override
    protected void                  onMetaDataUpdated() {
        super.onMetaDataUpdated();
        
        onOpen(false);
    }

    @Override
    public StreamOptions            getStreamOptions() {
        StreamOptions options = super.getStreamOptions();

        if (bufferOptions != null) {
            options.bufferOptions = new BufferOptions();
            options.bufferOptions.lossless = bufferOptions.lossless;
            options.bufferOptions.maxBufferSize = bufferOptions.maxBufferSize;
            options.bufferOptions.initialBufferSize = bufferOptions.initialBufferSize;
            options.bufferOptions.maxBufferTimeDepth = bufferOptions.maxBufferTimeDepth;
        }

        return options;
    }

    public final synchronized MessageEncoder <InstrumentMessage>   createEncoder(RecordClassDescriptor rcd) {
        
        if (isPolymorphic()) {
            RecordClassDescriptor[] rcds = getPolymorphicDescriptors ();
            assert rcds != null;
            boolean found = false;

            for (int i = 0; i < rcds.length; i++) {
                if (rcd.fieldsEquals(rcds[i]))
                    found = true;
                else
                    rcds[i] = null;
            }

            return found ?
                    new SimpleBoundEncoder(getCodecFactory (true),  TypeLoaderImpl.DEFAULT_INSTANCE, rcds) :
                    null;
        }

        return null;
    }

    @Override
    public void delete(TimeStamp from, TimeStamp to, IdentityKey... ids) {
        throw new UnsupportedOperationException("Not supported");
    }

    //
    //   Additional implementation
    //
    public BufferOptions            getBufferOptions () {
        return bufferOptions;
    }

    @Override
    @SuppressWarnings ("unchecked")
    MessageChannel <InstrumentMessage>          createChannel (
        InstrumentMessage                           msg,
        LoadingOptions                              options
    )
    {
        MessageEncoder<InstrumentMessage> encoder;
        if (options.raw)
             encoder = new SimpleRawEncoder(getTypes());
        else
            encoder = new SimpleBoundEncoder(
                getCodecFactory (options.channelQOS == ChannelQualityOfService.MIN_INIT_TIME),
                options.getTypeLoader(),
                getTypes());

        return (queue.getWriter (encoder));
    }

    @Override
    public void truncate(long time, IdentityKey... ids) {
        throw new UnsupportedOperationException("Not supported for TRANSIENT streams");
    }

    @Override
    public void             clear(IdentityKey... ids) {
        //throw new UnsupportedOperationException();
    }

    @Override
    public void             setHighAvailability(boolean value) {
    }

    @Override
    public boolean          getHighAvailability() {
        return false;
    }

    @Override
    public IdentityKey[] getComposition(IdentityKey... ids) {
        return ids;
    }

    @Override
    public TimeInterval[]       listTimeRange(IdentityKey... entities) {
        return null;
    }
}
