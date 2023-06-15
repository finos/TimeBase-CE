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
package com.epam.deltix.data.stream;

import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.stream.MessageSorter;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.progress.ConsoleProgressIndicator;
import org.junit.Test;

import java.io.File;
import java.util.Random;

/**
 * Created by Alex Karpovich on 4/3/2018.
 */
public class Test_MessageSorter {

    @Test
    public void testSimple() throws Exception {
        test(10000, 4);
    }

    public void test(long memorySize, int numGen) throws Exception {

        String tempDir = System.getProperty("java.io.tmpdir");

        System.out.println(tempDir);

        MessageSorter sorter =
                new MessageSorter (memorySize, new File(tempDir), TypeLoaderImpl.DEFAULT_INSTANCE);

        try {
            Random r = new Random (2009);
            TradeMessage msg = new TradeMessage();

            msg.setSymbol("DLTX");

            ConsoleProgressIndicator cpi = new ConsoleProgressIndicator ();

            cpi.setPrefix ("Adding: [");
            cpi.setTotalWork (numGen);

            for (int ii = 0; ii < numGen; ii++) {
                msg.setTimeStampMs(r.nextInt (946080000) * 1000L);
                msg.setPrice(ii);

                sorter.add (msg);

                cpi.setWorkDone (ii + 1);
            }

            MessageSource<InstrumentMessage> cur = sorter.finish ();
            int             numRead = 0;
            long            ts = -1;

            cpi.setPrefix ("Sorting: [");
            cpi.setWorkDone (0);
            cpi.setTotalWork (sorter.getTotalNumMessages ());

            while (cur.next ()) {
                InstrumentMessage imsg = cur.getMessage ();

                if (imsg.getTimeStampMs() < ts)
                    throw new RuntimeException (imsg.getTimeStampMs() + " < " + ts);

                ts = imsg.getTimeStampMs();
                numRead++;
                cpi.setWorkDone (numRead);
            }

            System.out.println ();

            if (numRead != numGen)
                throw new RuntimeException ();
        } finally {
            Util.close (sorter);
        }
    }
}