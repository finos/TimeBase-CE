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
package com.epam.deltix.qsrv.hf;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.qsrv.hf.topic.consumer.SubscriptionWorker;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
public class Test_DirectChannelListener extends BaseTopicReadingTest {
    private AtomicBoolean running;

    @Test(timeout = TEST_TIMEOUT)
    public void test() throws Exception {
        executeTest();
    }

    @NotNull
    protected Runnable createReader(AtomicLong messagesReceivedCounter, String channel, int dataStreamId, List<RecordClassDescriptor> types, MessageValidator messageValidator) {
        AtomicBoolean runningFlag = new AtomicBoolean(true);
        running = runningFlag;
        return () -> {
            RatePrinter ratePrinter = new RatePrinter("Reader");
            SubscriptionWorker directMessageListener = new DirectReaderFactory().createListener(aeron, false, channel, dataStreamId, types, message -> {
                messageValidator.validate(message);
                ratePrinter.inc();
                messagesReceivedCounter.incrementAndGet();
            }, null, StubData.getStubMappingProvider());
            ratePrinter.start();
            directMessageListener.processMessagesWhileTrue(runningFlag::get);
        };
    }

    protected void stopReader() {
        running.set(false);
    }
}