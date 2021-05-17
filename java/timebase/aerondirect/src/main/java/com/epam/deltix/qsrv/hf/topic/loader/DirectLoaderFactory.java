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
package com.epam.deltix.qsrv.hf.topic.loader;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.topic.DirectProtocol;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;
import com.epam.deltix.util.memory.MemoryDataOutput;
import io.aeron.Aeron;
import io.aeron.ExclusivePublication;
import io.aeron.Subscription;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.OutputStream;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class DirectLoaderFactory {
    private final CodecFactory codecFactory;
    private final TypeLoader typeLoader;

    public DirectLoaderFactory(CodecFactory codecFactory, TypeLoader typeLoader) {
        this.codecFactory = codecFactory;
        this.typeLoader = typeLoader;
    }

    public DirectLoaderFactory(CodecFactory codecFactory) {
        this(codecFactory, TypeLoaderImpl.DEFAULT_INSTANCE);
    }

    public DirectLoaderFactory() {
        this(CodecFactory.newCompiledCachingFactory());
    }

    public MessageChannel<InstrumentMessage> create(Aeron aeron, boolean raw, String publisherChannel, String metadataSubscriberChannel, int dataStreamId, int serverMetadataUpdatesStreamId, List<RecordClassDescriptor> typeList, byte loaderNumber, OutputStream outputStream, List<ConstantIdentityKey> mapping, @Nullable Runnable closeCallback, @Nullable IdleStrategy publicationIdleStrategy) {
        if (publicationIdleStrategy == null) {
            publicationIdleStrategy = new YieldingIdleStrategy();
        }

        ExclusivePublication publication = aeron.addExclusivePublication(publisherChannel, dataStreamId);
        Subscription serverMetadataUpdates = aeron.addSubscription(metadataSubscriberChannel, serverMetadataUpdatesStreamId);

        //recordClassSet.addClasses();

        MessageChannel<MemoryDataOutput> serverPublicationChannel = new MemoryDataOutputStreamChannel(outputStream);
        int firstTempEntityIndex = DirectProtocol.getFirstTempEntryIndex(loaderNumber);
        //int minTempEntityIndex = DirectProtocol.getMinTempEntryIndex(1);
        //int maxTempEntityIndex = DirectProtocol.getMaxTempEntryIndex(1);
        RecordClassDescriptor[] types = typeList.toArray(new RecordClassDescriptor[0]);
        return new DirectLoaderChannel(publication, codecFactory, raw, typeLoader, firstTempEntityIndex, serverPublicationChannel, serverMetadataUpdates, types, mapping, closeCallback, publicationIdleStrategy);
    }

}
