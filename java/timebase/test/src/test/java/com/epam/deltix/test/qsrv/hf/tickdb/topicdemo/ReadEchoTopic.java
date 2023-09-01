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
package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo;

import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.DemoConf;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.ExperimentFormat;
import org.agrona.concurrent.IdleStrategy;

import java.util.function.BooleanSupplier;

/**
 * @author Alexei Osipov
 */
public class ReadEchoTopic extends ReadEchoBase {
    public ReadEchoTopic(ExperimentFormat timestampGetter) {
        super(timestampGetter);
    }

    @Override
    protected void work(RemoteTickDB client, BooleanSupplier stopCondition, MessageProcessor processor) {
        String topicKey = experimentFormat.useMainChannel() ? DemoConf.DEMO_MAIN_TOPIC :  DemoConf.DEMO_ECHO_TOPIC;
        MessagePoller messagePoller = client.getTopicDB().createPollingConsumer(topicKey, null);

        try {
            IdleStrategy idleStrategy = DemoConf.getReaderIdleStrategy();
            while (!stopCondition.getAsBoolean()) {
                idleStrategy.idle(messagePoller.processMessages(100, processor));
            }
        } finally {
            messagePoller.close();
        }
    }
}