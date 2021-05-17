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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.util.lang.MathUtil;

import java.util.Comparator;
import java.util.PriorityQueue;

class MessageAccumulator extends PriorityQueue<UnprocessedMessage> {

    MessageAccumulator(int initialCapacity) {
        super(initialCapacity, UMC);
    }

    private volatile int sequence = 0;

    private static final Comparator<UnprocessedMessage> UMC =
        new Comparator <UnprocessedMessage> () {
            public int compare (UnprocessedMessage o1, UnprocessedMessage o2) {
                int result = MathUtil.compare(o1.nanos, o2.nanos);
                if (result == 0)
                    return MathUtil.compare (o1.index, o2.index);
                return result;
            }
        };

    @Override
    public boolean add(UnprocessedMessage msg) {
        msg.index = sequence++;
        return super.add(msg);
    }

    @Override
    public boolean offer(UnprocessedMessage msg) {
        msg.index = sequence++;
        return super.offer(msg);
    }
}

class UnprocessedMessage {
    final long      nanos;
    final byte []   data;
    int             index;
    final long      serial;

    public UnprocessedMessage (long nanos, long serial,
                               byte [] src, int offset, int length) {
        this.serial = serial;
        this.nanos = nanos;
        data = new byte [length];
        System.arraycopy (src, offset, data, 0, length);
    }
}
