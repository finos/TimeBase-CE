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
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.DirectChannel;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBTestBase;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDB.class)
public class Test_TopicAPI extends TDBTestBase {

    public Test_TopicAPI() {
        super(true, true, getTemporaryLocation(), new TomcatServer(null, 0, 0, true), true);
    }

    @Test(timeout = BaseTimeBaseTopicReadingTest.TIMEBASE_START_TIME_MS + 10000)
    public void test() throws Exception {
        DXTickDB db = getTickDb();

        TopicDB topicDB = db.getTopicDB();

        assertNotNull(topicDB.listTopics());

        String topicKey1 = "testTopic1";
        DirectChannel topic = topicDB.createTopic(topicKey1, new RecordClassDescriptor[]{
                StubData.makeTradeMessageDescriptor(),
                Messages.ERROR_MESSAGE_DESCRIPTOR
        }, null);

        List<String> foundTopics = topicDB.listTopics();
        assertEquals(List.of(topicKey1), foundTopics);
    }
}
