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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark;

import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark.channel.ChannelAccessor;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * @author Alexei Osipov
 */
abstract class ChannelBenchmarkBase {
    @SuppressFBWarnings(value = "PREDICTABLE_RANDOM", justification = "Random is used for internal tests, not cryptography")
    @NotNull
    static BinaryPayloadMessage createInstrumentMessage(int payloadSize) {
        BinaryPayloadMessage instrumentMessage = new BinaryPayloadMessage();
        instrumentMessage.setSymbol("TEST");
        ByteArrayList payload = instrumentMessage.getPayload();
        if (payloadSize > 0) {
            // Populate payload with randomly generated binary data to avoid compression effects
            Random random = new Random(0);
            byte[] bytes = new byte[payloadSize];
            random.nextBytes(bytes);
            payload.addAll(bytes, 0, payloadSize);
        }
        return instrumentMessage;
    }

    static String createChannel(ChannelAccessor accessor, RemoteTickDB tickDB) {
        String channelKey = "benchmark_tmp_" + RandomStringUtils.randomAlphanumeric(16);
        accessor.createChannel(tickDB, channelKey, new RecordClassDescriptor[]{getDescriptorForInstrumentMessage()});
        return channelKey;
    }

    private static RecordClassDescriptor getDescriptorForInstrumentMessage() {
        Introspector ix = Introspector.createEmptyMessageIntrospector();
        try {
            return ix.introspectRecordClass("Get RD for ChannelBenchmarkBase", BinaryPayloadMessage.class);
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
}