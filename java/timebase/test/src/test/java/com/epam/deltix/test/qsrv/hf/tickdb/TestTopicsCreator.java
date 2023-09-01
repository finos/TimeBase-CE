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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;

/**
 * Let's you create topics of different types for tests.
 * @author Alexei Osipov
 */
public class TestTopicsCreator {
    // timebase_host port topic_key topic_type [publisherAddress]
    public static void main(String[] args) {
        String timeBaseHost = args[0];
        int timeBasePort = Integer.parseInt(args[1]);
        String topicKey = args[2];
        String topicType = args[3];


        RemoteTickDB client = TickDBFactory.connect(timeBaseHost, timeBasePort, false);
        client.open(false);

        TopicSettings settings;
        switch (topicType) {
            case "ipc":
                settings = new TopicSettings();
                break;
            case "multicast":
                settings = new TopicSettings().setMulticastSettings(null);
                break;
            case "singleudp":
                String publisherAddress = args[4];
                settings = new TopicSettings().setSinglePublisherUdpMode(publisherAddress);
                break;
            default:
                throw new IllegalArgumentException("Wrong topicType");
        }


        TopicDB topicDB = client.getTopicDB();
        topicDB.createTopic(topicKey, new RecordClassDescriptor[]{TestTopicsStandalone.makeTradeMessageDescriptor()}, settings);

        client.close();
    }
}