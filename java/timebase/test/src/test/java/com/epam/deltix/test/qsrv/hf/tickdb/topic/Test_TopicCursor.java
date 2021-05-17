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
package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDB.class)
public class Test_TopicCursor extends BaseTimeBaseTopicReadingTest {
    private MessageSource<InstrumentMessage> messageSource;

    @Test(timeout = TEST_TIMEOUT)
    public void test() throws Exception {
        executeTest();
    }

    @NotNull
    protected Runnable createReader(AtomicLong messagesReceivedCounter, MessageValidator messageValidator, String topicKey, TopicDB topicDB) {

        this.messageSource = topicDB.createConsumer(topicKey, null, null);

        return () -> {
            RatePrinter ratePrinter = new RatePrinter("Reader");
            ratePrinter.start();
            try {
                while (messageSource.next()) {
                    messageValidator.validate(messageSource.getMessage());
                    ratePrinter.inc();
                    messagesReceivedCounter.incrementAndGet();
                }
            } catch (CursorIsClosedException ignore) {
            }

            messageSource.close();
        };
    }

    protected void stopReader() {
        messageSource.close();
    }

}