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

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.DemoConf;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.ExperimentFormat;
import com.epam.deltix.util.concurrent.CursorIsClosedException;

import java.util.function.BooleanSupplier;

/**
 * @author Alexei Osipov
 */
public class ReadEchoStream extends ReadEchoBase {
    private final ChannelPerformance channelPerformance;

    private TickCursor cursor = null;

    public ReadEchoStream(ChannelPerformance channelPerformance, ExperimentFormat experimentFormat) {
        super(experimentFormat);
        this.channelPerformance = channelPerformance;
    }

    @Override
    protected void work(RemoteTickDB client, BooleanSupplier stopCondition, MessageProcessor messageProcessor) {
        SelectionOptions options = new SelectionOptions();
        options.channelPerformance = this.channelPerformance;
        options.live = true;
        String streamKey = experimentFormat.useMainChannel() ? DemoConf.DEMO_MAIN_STREAM :  DemoConf.DEMO_ECHO_STREAM;
        this.cursor = client.getStream(streamKey).createCursor(options);
        cursor.subscribeToAllEntities();
        cursor.subscribeToAllTypes();
        cursor.reset(Long.MIN_VALUE);

        // Process messages from topic until stop signal triggered
        try {
            while (!stopCondition.getAsBoolean() && cursor.next()) {
                messageProcessor.process(cursor.getMessage());
            }
        } catch (CursorIsClosedException exception) {
            System.out.println("Echo reader cursor was closed");
        } finally {
            cursor.close();
        }
    }

    @Override
    public void stop() {
        // This is necessary because reader may block indefinitely
        if (cursor != null) {
            cursor.close();
        }
    }
}