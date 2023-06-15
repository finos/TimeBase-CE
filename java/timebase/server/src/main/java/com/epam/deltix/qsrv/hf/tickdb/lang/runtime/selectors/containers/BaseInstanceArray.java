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

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.codec.cg.ObjectPool;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.ARRT;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.Instance;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.util.List;
import java.util.function.Supplier;

public abstract class BaseInstanceArray<V, T extends List<V>> extends Instance implements ArrayContainer<T> {

    protected final T tempArray;
    protected final MemoryDataInput in = new MemoryDataInput();
    protected final MemoryDataOutput out = new MemoryDataOutput();

    protected T array;
    protected boolean changed = false;
    protected boolean decoded = true;

    public BaseInstanceArray(Supplier<T> supplier) {
        tempArray = supplier.get();
    }

    @Override
    public T get() {
        decode();

        if (array == null && isNull()) {
            return null;
        }

        if (array == null) {
            array = tempArray;
            this.in.setBytes(bytes(), offset(), length());
            array.clear();
            decode(array, in);
        }

        return array;
    }

    public void addAll(T value) {
        setInstance();
        setChanged();
        if (value != null) {
            get().addAll(value);
        }
    }

    public ObjectPool<? extends V> getPool() {
        return null;
    }

    public void setInstance() {
        if (array == null) {
            array = tempArray;
            setChanged();
        }
    }

    public void setChanged() {
        changed = true;
    }

    public void setList(T list) {
        setArray(list);
        setChanged();
    }

    public void setTypedList(List list) {
        throw new UnsupportedOperationException("Types are not supported");
    }

    @Override
    public void encode(MemoryDataOutput out) {
        encode();
        encodeArray(out);
    }

    protected void encode() {
        if (changed) {
            changed = false;
            if (array == null) {
                reset();
            } else {
                out.reset();
                encode(array, out);
                set(NULL_TYPE, out);
            }
        }
    }

    @Override
    public void decode(MemoryDataInput mdi) {
        decoded = false;
        super.decodeArray(mdi);
    }

    @Override
    public boolean isNull() {
        return (array == null && super.isNull());
    }

    public void addAllFromInput(MemoryDataInput mdi) {
        MessageSizeCodec.read(mdi); // message size
        setInstance();
        setChanged();
        decode(array, mdi);
    }

    public void decode() {
        if (!decoded) {
            decoded = true;
            array = tempArray;
            array.clear();
            in.setBytes(bytes(), offset(), length());
            if (in.hasAvail()) {
                decode(array, in);
            }
        }
    }

    public void setNull() {
        reset();
        tempArray.clear();
        array = null;
    }

    public void setEmpty() {
        reset();
        tempArray.clear();
        setInstance();
    }

    protected void setArray(T array) {
        if (this.array != array)
            this.array = array;
    }

    protected int encode(T array, MemoryDataOutput mdo) {
        return ARRT.encodeArrayWithoutSize(array, mdo);
    }

    protected void decode(T array, MemoryDataInput mdi) {
        ARRT.decodeArray(array, mdi);
    }
}