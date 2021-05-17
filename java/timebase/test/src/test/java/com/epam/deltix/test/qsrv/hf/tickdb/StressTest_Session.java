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

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.Test;

import java.util.Random;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBStress;

@Category(TickDBStress.class)
public class StressTest_Session {

    @Test
    public void run() throws Throwable {

        TDBRunner runner = new ServerRunner(true,true);
        runner.startup();

        DXTickDB db = runner.getTickDb();

        DXTickStream stream = db.getStream("agg");
        if (stream != null)
            stream.delete();

        stream = db.createStream("agg", StreamOptions.fixedType(StreamScope.DURABLE, "agg", null, 0,
                StreamConfigurationHelper.mkTradeMessageDescriptor(null, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE))
        );

        Updater[] threads = new Updater[10];

        for (int i = 0; i < threads.length; i++) {

            IdentityKey[] symbols = new IdentityKey[10];
            for (int j = 0; j < symbols.length; j++)
                symbols[j] = new ConstantIdentityKey(i + "_" + j);

            threads[i] = new Updater(i, stream, symbols);
        }


        for (int i = 0; i < threads.length; i++)
            threads[i].start();

        for (int i = 0; i < threads.length; i++)
            threads[i].join();


        runner.shutdown();
    }
}

class Updater extends Thread {

    public final DXTickStream stream;
    public final IdentityKey[] symbols;

    Updater(int id, DXTickStream stream, IdentityKey[] symbols) {
        super("Updater:" + id);

        this.stream = stream;
        this.symbols = symbols;
    }

    @Override
    public void run() {

        int count = 0;

        Random rnd = new Random(this.getId());

        TradeMessage message = new TradeMessage();

        while (count++ < 500) {

            for (IdentityKey symbol : symbols) {

                long[] range = stream.getTimeRange(symbol);

                try (TickLoader loader = stream.createLoader(new LoadingOptions(false))) {
                    try {
                        Thread.sleep(rnd.nextInt(10));
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }

                    message.setSymbol(symbol.getSymbol().toString());
                    message.setTimeStampMs(range != null ? range[1] + 1 : TimeKeeper.currentTime);

                    message.setSize(count);
                    message.setPrice(count);

                    loader.send(message);
                }
            }

            try {
                //System.out.println(this + " upload instruments done (" + count + ")");
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
