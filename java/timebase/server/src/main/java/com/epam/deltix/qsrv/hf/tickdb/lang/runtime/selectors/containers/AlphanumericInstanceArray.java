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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers;

import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.ARRT;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

public class AlphanumericInstanceArray extends BaseInstanceArray<Long, LongArrayList> {

    private final AlphanumericCodec codec = new AlphanumericCodec(10);

    public AlphanumericInstanceArray() {
        super(LongArrayList::new);
    }

    public void addFromInput(MemoryDataInput mdi) {
        setInstance();
        setChanged();
        get().add(codec.readLong(mdi));
    }

    public void add(long value) {
        setInstance();
        setChanged();
        get().add(value);
    }

    @Override
    protected int encode(LongArrayList array, MemoryDataOutput mdo) {
        return ARRT.encodeArrayWithoutSize(array, mdo, codec);
    }

    @Override
    protected void decode(LongArrayList array, MemoryDataInput mdi) {
        ARRT.decodeArray(array, mdi, codec);
    }

}