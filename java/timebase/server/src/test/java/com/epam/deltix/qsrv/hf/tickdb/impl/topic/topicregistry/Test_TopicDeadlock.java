/*
 * Copyright 2023 EPAM Systems, Inc
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
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.concurrent.QuickExecutor;
import io.aeron.Aeron;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test for a Deadlock that can occur when there are multiple producers added to the topic at the same time.
 *
 * Note: this test requires running Aeron driver (see deltix.util.io.aeron.AeronDriver).
 * TODO: Make this test independent of external driver.
 *
 * @author Alexei Osipov
 */
//@Category(Object.class)
public class Test_TopicDeadlock {
    @Test(timeout = 30_000)
    public void test() throws Exception {
        TopicTestUtils.initTempQSHome();

        Aeron aeron = TopicTestUtils.createAeron();

        DirectTopicRegistry directTopicRegistry = new DirectTopicRegistry();
        List<RecordClassDescriptor> types = Collections.singletonList(Messages.ERROR_MESSAGE_DESCRIPTOR);

        String topicName = "testTopic";
        AtomicInteger streamIdGenerator = new AtomicInteger(new Random().nextInt());

        ConstantIdentityKey key = new ConstantIdentityKey("ABC");
        directTopicRegistry.createDirectTopic(topicName, types, null, streamIdGenerator::incrementAndGet, Collections.singletonList(key), TopicType.IPC, null, null);

        CountDownLatch latch = new CountDownLatch(1);
        QuickExecutor newInstance = QuickExecutor.createNewInstance("Test Executor", null);

        List<Thread> threads = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            int index = i;
            Thread thread = new Thread(() -> {
                CommunicationPipe pipe = new CommunicationPipe();
                InputStream loaderInputStream = pipe.getInputStream();

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                List<IdentityKey> keys = Collections.singletonList(
                        new ConstantIdentityKey("TEST-" + index)
                );
                directTopicRegistry.addLoader(topicName, loaderInputStream, keys, newInstance, aeron, true, null);
            });
            thread.setName("Test thread " + i);
            thread.start();
            threads.add(thread);
        }

        latch.countDown();

        for (Thread thread : threads) {
            thread.join(10_000);
            if (thread.isAlive()) {
                Assert.fail("At least on thread is blocked");
            }
        }
    }
}