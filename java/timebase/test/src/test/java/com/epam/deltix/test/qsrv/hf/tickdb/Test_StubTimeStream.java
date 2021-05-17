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

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
public class Test_StubTimeStream extends TDBTestBase {
    public Test_StubTimeStream() {
        super(true);
    }

    @Test
    public void testStubStream() throws Exception {
        DXTickDB tickDb = getTickDb();
        StreamOptions streamOptions = new StreamOptions();
        streamOptions.setFlag(TDBProtocol.AF_STUB_STREAM, true);
        streamOptions.scope = StreamScope.RUNTIME;
        DXTickStream stream = tickDb.createStream("stubStream", streamOptions);

        try (TickCursor cursor = stream.createCursor(new SelectionOptions())) {
            cursor.reset(Long.MIN_VALUE);
            measureSpeed(cursor, 100_000_000);
        }
    }

    private void measureSpeed(TickCursor cursor, long numberOfMessages) {
        long t0 = System.nanoTime();
        long count = 0;
        long blackHole = 0;
        while (count < numberOfMessages && cursor.next()) {
            count ++;
            // Touch message time to emulate message object access
            blackHole = blackHole & cursor.getMessage().getNanoTime();
        }
        long t1 = System.nanoTime();
        assert blackHole == 0;
        double speed = count * TimeUnit.SECONDS.toNanos(1) / (t1 - t0) + blackHole; // Msgs/sec
        System.out.println(String.format("Read speed: %.1f Mmsg/s", speed / 1_000_000));
    }
}