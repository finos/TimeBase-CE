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

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.MulticastTopicSettings;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDB.class)
public class Test_TopicMulticastPoller extends Test_TopicPollerBase {
    @Test(timeout = TEST_TIMEOUT)
    @Ignore // Disabled because this tests sends real multicast UDP traffic and can disturb other applications in same network.
    public void test() throws Exception {
        executeTest();
    }

    @Override
    protected void createTopic(TopicDB topicDB, String topicKey, RecordClassDescriptor[] types) {
        MulticastTopicSettings settings = new MulticastTopicSettings();
        settings.setTtl(1);
        topicDB.createTopic(topicKey, types,  new TopicSettings().setMulticastSettings(settings));
    }
}