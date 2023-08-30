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
package com.epam.deltix.test.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.DXRemoteDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.thread.affinity.AffinityConfig;
import org.junit.Test;

import java.util.BitSet;

/**
 * Utility class to help with testing thread affinity implementation (Issue #5467).
 *
 * This is trivial case of a client that works with external TB instance. As is, it not really tests anything.
 *
 * To check if defined affinity works you have to use an external tool that is able to show thread affinity.
 * Also you may want to setup some breakpoint in IDE on AffinityThreadFactoryBuilder class to observe thread creation.
 *
 * @author Alexei Osipov
 */
public class TickDBClientAffinityTest {

    private static final String TRANSIENT_KEY = "tmp";
    private static final int PORT = 8056;

    private static RecordClassDescriptor mmDescriptor = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
    private static RecordClassDescriptor tradeDescriptor = StreamConfigurationHelper.mkTradeMessageDescriptor(
            mmDescriptor, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

    @Test
    public void test() throws Exception {
        String url = "dxtick://localhost:" + PORT;
        System.out.println("Connecting to " + url + "...");

        DXTickDB db = TickDBFactory.createFromUrl(url);
        ((DXRemoteDB) db).setAffinityConfig(new AffinityConfig(thread -> new BitSet(2)));
        db.open(false);

        final DXTickStream stream = createLiveTransStream(db);


        TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, false));
        final TickLoader loader = stream.createLoader(new LoadingOptions(true));

        loader.send(getMessage());
        cursor.next();

        db.close();
    }

    private static DXTickStream createLiveTransStream(DXTickDB db) {
        DXTickStream liveTrans = db.getStream(TRANSIENT_KEY);
        if (liveTrans != null) {
            System.out.println(TRANSIENT_KEY + " stream exists. Removing...");
            liveTrans.delete();
            System.out.println("OK");
        }

        StreamOptions options = StreamOptions.fixedType(StreamScope.TRANSIENT, TRANSIENT_KEY, TRANSIENT_KEY, 0, tradeDescriptor);
        options.bufferOptions = new BufferOptions();
        options.bufferOptions.initialBufferSize = 1024 << 10;
        options.bufferOptions.maxBufferSize = 1024 << 10;
        options.bufferOptions.lossless = true;
        return db.createStream(options.name, options);
    }


    private static RawMessage getMessage() {

        RawMessage msg = new RawMessage();
        msg.type = tradeDescriptor;
        msg.data = new byte[10];
        msg.setSymbol("AAA");
        return msg;
    }

}