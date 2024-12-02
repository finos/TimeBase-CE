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
package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.DirectChannel;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBRunnerBase;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBTestBase;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.JUnitCategories;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDBFast.class)
public class Test_TopicCopyToStream_Lock extends TDBTestBase {

    public static final int COUNT_1 = 1000;
    public static final int COUNT_2 = 2000;

    public Test_TopicCopyToStream_Lock() {
        super(true, true, getTemporaryLocation(), new TomcatServer(null, 0, 0, true), true);
    }

    @Test
    public void testLock() throws InterruptedException {
        DXTickDB db = getTickDb();

        String topicKey = "TopicReadingTest_" + RandomStringUtils.randomAlphanumeric(8);
        String streamKey = topicKey + "_stream";

        TopicDB topicDB = db.getTopicDB();
        if (topicDB.getTopic(topicKey) != null) {
            // Delete old topic version
            topicDB.deleteTopic(topicKey);
        }

        DXTickStream oldStream = db.getStream(streamKey);
        if (oldStream != null) {
            oldStream.delete();
        }
        // Test start
        StreamOptions streamOptions = new StreamOptions();
        streamOptions.setPolymorphic(StubData.makeTradeMessageDescriptor());
        DXTickStream stream = db.createStream(streamKey, streamOptions);


        RecordClassDescriptor[] types = {StubData.makeTradeMessageDescriptor()};


        DirectChannel topic = topicDB.createTopic(topicKey, types, new TopicSettings().setCopyToStream(streamKey));

        assertCanNotLock(stream);

        TradeMessage msg = new TradeMessage();
        msg.setSymbol("ABC");
        msg.setOriginalTimestamp(234567890);

        MessageChannel<InstrumentMessage> publisher = topic.createPublisher(getLoadingOptions());
        //msg.setTimeStampMs(System.currentTimeMillis());
        for (int i = 0; i < COUNT_1; i++) {
            publisher.send(msg);
        }
        publisher.close();

        topicDB.deleteTopic(topicKey);

        assertCanLock(stream);

        // Re-create topic
        topic = topicDB.createTopic(topicKey, types, new TopicSettings().setCopyToStream(streamKey));

        assertCanNotLock(stream);

        // Send one more message
        publisher = topic.createPublisher(getLoadingOptions());
        //msg.setTimeStampMs(System.currentTimeMillis());
        for (int i = 0; i < COUNT_2; i++) {
            publisher.send(msg);
        }
        publisher.close();

        // TODO: We should get rid of that
        Thread.sleep(100); // Let copy thread to see the messages
        topicDB.deleteTopic(topicKey);

        assertCanLock(stream);


        try (TickCursor cursor = db.getStream(streamKey).select(Long.MIN_VALUE, null)) {
            Assert.assertEquals(COUNT_1 + COUNT_2, TDBRunnerBase.countMessages(cursor));
        }

        db.getStream(streamKey).delete();
    }

    private void assertCanNotLock(DXTickStream stream) {
        boolean locked = false;
        boolean gotLockedException = false;
        try {
            stream.lock();
            locked = true;
        } catch (StreamLockedException e) {
            gotLockedException = true;
        }
        Assert.assertFalse(locked);
        Assert.assertTrue(gotLockedException);
    }

    private void assertCanLock(DXTickStream stream) {
        boolean locked = false;
        boolean gotLockedException = false;
        try {
            DBLock lock = stream.lock();
            locked = true;
            lock.release();
        } catch (StreamLockedException e) {
            gotLockedException = true;
        }
        Assert.assertTrue("Lock must be successful", locked);
        Assert.assertFalse(gotLockedException);
    }

    @NotNull
    private static LoadingOptions getLoadingOptions() {
        return new LoadingOptions(false, LoadingOptions.WriteMode.REWRITE);
    }
}

