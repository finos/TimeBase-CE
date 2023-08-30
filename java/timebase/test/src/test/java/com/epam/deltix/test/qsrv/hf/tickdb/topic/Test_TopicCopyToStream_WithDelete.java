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

import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBTestBase;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.DirectChannel;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;
import com.epam.deltix.util.JUnitCategories;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDBFast.class)
public class Test_TopicCopyToStream_WithDelete extends TDBTestBase {
    public Test_TopicCopyToStream_WithDelete() {
        super(true);
    }

    @Test(timeout = 30_000)
    public void testGetTopic() {
        DXTickDB db = getTickDb();

        String topicKey = "TopicReadingTest_" + RandomStringUtils.randomAlphanumeric(8);
        String streamKey = topicKey + "_stream";

        TopicDB topicDB = db.getTopicDB();
        if (topicDB.getTopic(topicKey) != null) {
            // Delete old topic version
            topicDB.deleteTopic(topicKey);
        }
        DirectChannel topic = topicDB.createTopic(topicKey, new RecordClassDescriptor[]{StubData.makeTradeMessageDescriptor()}, new TopicSettings().setCopyToStream(streamKey));


        TradeMessage msg = new TradeMessage();
        msg.setSymbol("ABC");
        msg.setOriginalTimestamp(234567890);

        MessageChannel<InstrumentMessage> publisher = topic.createPublisher(new LoadingOptions());
        msg.setTimeStampMs(System.currentTimeMillis());
        publisher.send(msg);
        publisher.close();

        db.close();
        db.open(false);

        topic = topicDB.getTopic(topicKey);
        assert topic != null;
        publisher = topic.createPublisher(new LoadingOptions());
        publisher.send(msg);
        publisher.close();

        topicDB.deleteTopic(topicKey);

        // Re-create topic
        topic = topicDB.createTopic(topicKey, new RecordClassDescriptor[]{StubData.makeTradeMessageDescriptor()}, new TopicSettings().setCopyToStream(streamKey));

        // Send one more message
        publisher = topic.createPublisher(new LoadingOptions());
        msg.setTimeStampMs(System.currentTimeMillis());
        publisher.send(msg);
        publisher.close();

        topicDB.deleteTopic(topicKey);

        //Thread.sleep(10_000);

        TickCursor cursor = db.getStream(streamKey).select(Long.MIN_VALUE, null);

        int messagesInStream = 0;
        while (cursor.next()) {
            messagesInStream ++;
        }
        Assert.assertEquals(3, messagesInStream);

        cursor.close();
        db.getStream(streamKey).delete();
    }
}