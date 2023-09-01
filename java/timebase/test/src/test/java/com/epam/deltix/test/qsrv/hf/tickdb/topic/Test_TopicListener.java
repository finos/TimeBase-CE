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
package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.lang.Disposable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDB.class)
public class Test_TopicListener extends BaseTimeBaseTopicReadingTest {
    private Disposable worker;

    @Test(timeout = TEST_TIMEOUT)
    public void test() throws Exception {
        executeTest();
    }

    @NotNull
    protected Runnable createReader(AtomicLong messagesReceivedCounter, MessageValidator messageValidator, String topicKey, TopicDB topicDB) {
        return () -> {
            RatePrinter ratePrinter = new RatePrinter("Reader");
            ratePrinter.start();
            this.worker = topicDB.createConsumerWorker(topicKey, null, null, null, message -> {
                messageValidator.validate(message);
                ratePrinter.inc();
                messagesReceivedCounter.incrementAndGet();
            });
        };
    }

    protected void stopReader() {
        worker.close();
    }
}