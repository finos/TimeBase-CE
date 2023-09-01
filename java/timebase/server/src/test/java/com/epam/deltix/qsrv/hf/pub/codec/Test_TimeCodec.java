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
package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.JUnitCategories.UHFFramework;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author Andy
 *         Date: 6/13/11 9:25 AM
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_TimeCodec {


    @Test
    public void test () {
        final MemoryDataOutput out = new MemoryDataOutput(1024);
        final byte [] buffer = out.getBuffer();
        final MemoryDataInput in = new MemoryDataInput(buffer);

        long time = System.currentTimeMillis() -  TimeUnit.DAYS.toMillis(1);
        final long endTime = time + TimeUnit.DAYS.toMillis(1);
        while (time < endTime){
            TimeCodec.writeTime(time, out);
            long timeRead = TimeCodec.readTime(in);

            assertEquals ("Times match", time, timeRead);

            time++;
            out.reset();
            in.seek(0);
        }
    }

    @Test
    public void test1 () {
        final MemoryDataOutput out = new MemoryDataOutput(1024);
        final byte [] buffer = out.getBuffer();
        final MemoryDataInput in = new MemoryDataInput(buffer);

        InstrumentMessage msg = new InstrumentMessage();
        msg.setNanoTime(TimeKeeper.currentTimeNanos);
        
        TimeCodec.writeTime(msg, out);
        long nanos = TimeCodec.readNanoTime(in);

        assertEquals ("Times match", nanos, msg.getNanoTime());

//            time++;
//            out.reset();
//            in.seek(0);
        
    }

    @Test
    public void test3 () {
        final MemoryDataOutput out = new MemoryDataOutput(1024);
        final byte [] buffer = out.getBuffer();
        int j = 0;
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            buffer[j] = (byte)i;
            j++;
        }

        final MemoryDataInput in = new MemoryDataInput(buffer);
        for (int i = 0; i < 256; i++) {
            in.seek(i);
            int b = in.readUnsignedByte();
            in.seek(i);
            long l = in.readLongUnsignedByte();
            if (b != l) {
                throw new IllegalStateException();
            }
            assertEquals (l, b);
        }
    }
}