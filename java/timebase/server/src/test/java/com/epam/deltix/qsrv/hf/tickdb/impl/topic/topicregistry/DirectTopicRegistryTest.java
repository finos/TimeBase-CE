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
package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CommunicationPipe;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import com.epam.deltix.qsrv.hf.topic.loader.DirectTopicLoaderCodec;
import com.epam.deltix.qsrv.hf.topic.loader.MemoryDataOutputStreamChannel;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.memory.MemoryDataOutput;
import io.aeron.Aeron;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 *
 * Note: this test requires running Aeron driver (see deltix.util.io.aeron.AeronDriver).
 * TODO: Make this test independent of external driver.
 *
 * @author Alexei Osipov
 */
public class DirectTopicRegistryTest {

    @Test //(timeout = 5_000)
    public void getTopicTemporaryMappingSnapshot() throws InterruptedException, IOException {
        TopicTestUtils.initTempQSHome();

        ConstantIdentityKey key = new ConstantIdentityKey("ABC");

        Aeron aeron = TopicTestUtils.createAeron();
        QuickExecutor newInstance = QuickExecutor.createNewInstance("Test Executor", null);

        DirectTopicRegistry directTopicRegistry = new DirectTopicRegistry();
        List<RecordClassDescriptor> types = Collections.singletonList(Messages.ERROR_MESSAGE_DESCRIPTOR);

        String topicName = "testTopic";
        AtomicInteger streamIdGenerator = new AtomicInteger(new Random().nextInt());



        directTopicRegistry.createDirectTopic(topicName, types, null, streamIdGenerator::incrementAndGet, Collections.emptyList(), TopicType.IPC, null, null);

        int tempIndex = -555;

        CommunicationPipe pipe = new CommunicationPipe();
        InputStream loaderInputStream = pipe.getInputStream();

        LoaderSubscriptionResult loaderSubscriptionResult = directTopicRegistry.addLoader(topicName, loaderInputStream, Collections.emptyList(), newInstance, aeron, true, null);

        MemoryDataOutput mdo = new MemoryDataOutput();
        OutputStream outputStream = pipe.getOutputStream();
        MemoryDataOutputStreamChannel outputStreamChannel = new MemoryDataOutputStreamChannel(outputStream);

        DirectTopicLoaderCodec.writeSingleEntryInstrumentMetadata(mdo, key.symbol, tempIndex);
        outputStreamChannel.send(mdo);

        loaderSubscriptionResult.getDataAvailabilityCallback().run(); // Trigger message processing from loader
        Thread.sleep(100); // Let DirectTopicHandler to handle the message

        IntegerToObjectHashMap<ConstantIdentityKey> mapping = directTopicRegistry.getTopicTemporaryMappingSnapshot(topicName, tempIndex);

        assertTrue(mapping.size() > 0);
        ConstantIdentityKey foundKey = mapping.get(tempIndex, null);
        assertNotNull(foundKey);
        assertEquals(foundKey, key);
    }
}