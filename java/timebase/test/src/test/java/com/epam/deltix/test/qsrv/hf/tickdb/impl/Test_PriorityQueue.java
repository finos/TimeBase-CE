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
package com.epam.deltix.test.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.impl.TimestampPriorityQueue;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import org.junit.Test;
import org.junit.Assert;


import java.io.StringWriter;
import java.io.IOException;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 * User: BazylevD
 * Date: Apr 17, 2009
 */
@Category(TickDBFast.class)
public class Test_PriorityQueue {

    private static final long[] TIMESTAMPS = {1, 2, 3, 4, 4, 4, 2, 10, 5, 4, 50};
    private static final String ETALON =
            "1 0\n"+
            "2 1\n"+
            "2 6\n"+
            "3 2\n"+
            "4 3\n"+
            "4 4\n"+
            "4 5\n"+
            "4 9\n"+
            "5 8\n"+
            "10 7\n"+
            "50 10\n";

    @Test
    public void test() throws IOException {
        final TimestampPriorityQueue<TradeMessage> queue = new TimestampPriorityQueue<TradeMessage>();

        // fill
        TradeMessage msg = new TradeMessage();
        msg.setSymbol("");
        int idx = 0;
        for (long ts : TIMESTAMPS) {
            msg = (TradeMessage)msg.clone();
            msg.setTimeStampMs(ts);
            msg.setSize(idx++);
            queue.offer(msg);
        }

        // query
        StringWriter wr = new StringWriter(ETALON.length());
        while (queue.size() > 0) {
            msg = queue.poll();
            if (msg != null)
                wr.write(msg.getTimeStampMs() + " " + (int) msg.getSize() + '\n');
            else
                wr.write("null\n");
        }
        wr.close();

        Assert.assertEquals(ETALON, wr.toString());
    }
}