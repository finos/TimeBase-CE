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
package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.DemoConf;
import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;

/**
 * @author Alexei Osipov
 */
public class WriteTopic extends WriteBase {
    @Override
    protected MessageChannel<InstrumentMessage> createLoader(RemoteTickDB client) {
        return client.getTopicDB().createPublisher(DemoConf.DEMO_MAIN_TOPIC, null, new BusySpinIdleStrategy());
    }
}