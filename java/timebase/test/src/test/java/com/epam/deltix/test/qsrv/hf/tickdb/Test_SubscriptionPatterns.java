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

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Test_SubscriptionPatterns extends TDBRunnerBase {

    private DXTickStream getBars () {
        DXTickDB db = getTickDb();

        DXTickStream tickStream = db.getStream(TickDBCreator.BARS_STREAM_KEY);
        if (tickStream == null)
            tickStream = TickDBCreator.createBarsStream (db, TickDBCreator.BARS_STREAM_KEY);

        return tickStream;
    }

    @Test
    public void test() {
        try (TickCursor cursor = getBars().select(0, new SelectionOptions(), null,
                new IdentityKey[] { new ConstantIdentityKey("*") }))
        {
            assertEquals (94851, countMessages (cursor));
        }
    }

    private int             countMessages (TickCursor cursor) {
        int                 count = 0;

        while (cursor.next ())
            count++;

        return (count);
    }

}