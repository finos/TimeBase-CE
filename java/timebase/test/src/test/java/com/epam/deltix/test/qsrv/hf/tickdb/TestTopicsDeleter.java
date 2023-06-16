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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;

/**
 * Simple tool that deletes specified topic.
 *
 * @author Alexei Osipov
 */
public class TestTopicsDeleter {
    // timebase_host port topic_key
    public static void main(String[] args) {
        String timeBaseHost = args[0];
        int timeBasePort = Integer.parseInt(args[1]);
        String topicKey = args[2];


        RemoteTickDB client = TickDBFactory.connect(timeBaseHost, timeBasePort, false);
        client.open(false);


        TopicDB topicDB = client.getTopicDB();
        topicDB.deleteTopic(topicKey);
        client.close();
    }
}