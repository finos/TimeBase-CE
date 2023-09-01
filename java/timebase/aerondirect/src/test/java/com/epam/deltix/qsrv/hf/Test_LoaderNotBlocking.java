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
package com.epam.deltix.qsrv.hf;

import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.streaming.MessageChannel;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.topic.loader.DirectLoaderFactory;
import com.epam.deltix.timebase.messages.service.ErrorMessage;
import io.aeron.CommonContext;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Alexei Osipov
 */
@Category(Object.class)
public class Test_LoaderNotBlocking extends BaseAeronTest {

    /**
     * Tests that message sending does not block when there are no consumers.
     */
    @Test(timeout = 30_000)
    public void testMissingConsumer() {
        String channel = CommonContext.IPC_CHANNEL;
        int dataStreamId = new Random().nextInt();
        int serverMetadataStreamId = dataStreamId + 1;
        List<RecordClassDescriptor> types = Collections.singletonList(Messages.ERROR_MESSAGE_DESCRIPTOR);
        byte loaderNumber = 1;

        MessageChannel<InstrumentMessage> messageChannel = new DirectLoaderFactory().create(aeron, false, channel, channel, dataStreamId, serverMetadataStreamId, types, loaderNumber, new ByteArrayOutputStream(8 * 1024), Collections.emptyList(), null, null);

        ErrorMessage msg = new ErrorMessage();
        msg.setSymbol("ABC");
        msg.setMessageText("Not exists");

        for (int i = 0; i < 1_000_000; i++) {
            messageChannel.send(msg);
        }
        messageChannel.close();
    }
}