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

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBTestBase;
import com.epam.deltix.test.qsrv.hf.tickdb.TestTopicsStandalone;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.PublisherPreferences;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicNotFoundException;
import com.epam.deltix.util.JUnitCategories.TickDBFast;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@Category(TickDBFast.class)
public class Test_PublisherInitialEntities extends TDBTestBase {


    public Test_PublisherInitialEntities() {
        super(true);
    }

    @Test(timeout = 30_000)
    public void testBlock () {
        RemoteTickDB db = (RemoteTickDB) getTickDb();

        String topicKey = "Test_PublisherInitialEntities";
        try {
            db.getTopicDB().deleteTopic(topicKey);
        } catch (TopicNotFoundException ignored) {
        }

        db.getTopicDB().createTopic(topicKey, new RecordClassDescriptor[]{TestTopicsStandalone.makeTradeMessageDescriptor()}, null);

        List<IdentityKey> initialEntitySet = Arrays.asList(
                new ConstantIdentityKey("GOOG"),
                new ConstantIdentityKey("AAPL")
        );
        MessageChannel<InstrumentMessage> publisher = db.getTopicDB().createPublisher(topicKey, new PublisherPreferences().setInitialEntitySet(initialEntitySet), new YieldingIdleStrategy());
        publisher.close();
    }


}