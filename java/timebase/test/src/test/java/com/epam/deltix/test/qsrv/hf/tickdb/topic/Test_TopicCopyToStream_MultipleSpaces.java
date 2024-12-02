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
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.PublisherPreferences;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Runs multiple producers that utilize {@link PublisherPreferences#setPreserveNullTimestamp} feature.
 */
@Category(JUnitCategories.TickDB.class)
public class Test_TopicCopyToStream_MultipleSpaces extends Test_TopicPollerBase {
    private final String copyToStreamKey = "stream1";

    @Test(timeout = TEST_TIMEOUT + 30_000)
    //@Ignore // This test can create a significant amount of data (like 1Gb) and put big pressure on the disk drive. So it's disabled by default.
    public void test() throws Exception {
        DXTickDB tickDb = runner.getTickDb();
        executeTest(3);
        DXTickStream stream = tickDb.getStream(copyToStreamKey);
        try {
            TickCursor cursor = stream.createCursor(new SelectionOptions());
            cursor.subscribeToAllEntities();
            cursor.subscribeToAllTypes();
            cursor.reset(Long.MIN_VALUE);
            long count = 0;
            while (cursor.next()) {
                count++;
            }
            cursor.close();
            Assert.assertEquals(this.finalMessageSentCount.longValue(), count);
        } finally {
            stream.delete();
        }
    }

    protected void setMessageTimestamp(TradeMessage msg, long messageSentCounter) {
        msg.setTimeStampMs(TimeStampedMessage.TIMESTAMP_UNKNOWN);
    }

    @Override
    protected PublisherPreferences getPublisherPreferences() {
        return new PublisherPreferences()
                .setPreserveNullTimestamp(true);
    }

    @Override
    protected void createTopic(TopicDB topicDB, String topicKey, RecordClassDescriptor[] types) {
        String space = "sp_" + topicKey;
        topicDB.createTopic(topicKey, types,  new TopicSettings().setCopyToStream(copyToStreamKey, space));
    }
}
