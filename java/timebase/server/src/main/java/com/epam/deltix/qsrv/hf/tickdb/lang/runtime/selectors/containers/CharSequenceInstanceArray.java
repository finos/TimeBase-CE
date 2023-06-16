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

import com.epam.deltix.qsrv.hf.codec.cg.StringBuilderPool;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.ARRT;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

public class CharSequenceInstanceArray extends BaseInstanceArray<CharSequence, ObjectArrayList<CharSequence>> {

    protected final StringBuilderPool pool;

    public CharSequenceInstanceArray(StringBuilderPool pool) {
        super(ObjectArrayList::new);
        this.pool = pool;
    }

    public CharSequenceInstanceArray(CharSequence[] array) {
        super(() -> new ObjectArrayList<>(array));
        this.pool = null;
        setInstance();
    }

    public void addFromInput(MemoryDataInput mdi) {
        setInstance();
        setChanged();
        get().add(mdi == null ? null: mdi.readStringBuilder(pool.borrow()));
    }

    public void add(CharSequence cs) {
        setInstance();
        setChanged();
        get().add(cs);
    }

    @Override
    public StringBuilderPool getPool() {
        return pool;
    }

    @Override
    protected int encode(ObjectArrayList<CharSequence> array, MemoryDataOutput mdo) {
        return ARRT.encodeArrayWithoutSize(array, mdo);
    }

    @Override
    protected void decode(ObjectArrayList<CharSequence> array, MemoryDataInput mdi) {
        ARRT.decodeArray(array, mdi, pool);
    }
}