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

import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.ARRT;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.Instance;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.InstancePool;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *  Variable holding an array value.
 */
@SuppressWarnings("rawtypes")
public class InstanceArray extends BaseInstanceArray<Instance, ObjectArrayList<Instance>> implements InstanceArrayIterator {

    protected final InstancePool pool;
    protected final PolyArrayCodec polyArrayCodec;
    private int i = -1;

    protected final ObjectArrayList tempTypedArray = new ObjectArrayList();
    protected ObjectArrayList typedArray;
    protected boolean typedDecoded = false;
    protected boolean typedChanged = false;

    public InstanceArray(InstancePool pool) {
        super(ObjectArrayList::new);
        this.pool = pool;
        this.polyArrayCodec = null;
    }

    public InstanceArray(InstancePool pool, Class<?> ... classes) {
        super(ObjectArrayList::new);
        this.pool = pool;
        this.polyArrayCodec = new PolyArrayCodec(classes);
    }

    @Override
    public int startRead() {
        decode();
        i = -1;
        return array.size(); // read length
    }

    @Override
    public boolean next() {
        ++i;
        return getElement() != null;
    }

    @Override
    public Instance getElement() {
        return array.get(i);
    }

    public void add(Instance value) {
        setInstance();
        setChanged();
        array.add(value);
    }

    public Instance addCopy(Instance value) {
        setInstance();
        setChanged();

        Instance newValue = pool.borrow();
        newValue.set(value);
        array.add(newValue);
        return newValue;
    }

    public void addFromInput(MemoryDataInput mdi) {
        setInstance();
        setChanged();
        Instance instance = pool.borrow();
        instance.decode(mdi);
        array.add(instance);
    }

    @Override
    public void adjustTypeId(int adjustTypeIndex) {
        decode();
        for (int i = 0; i < array.size(); ++i) {
            array.get(i).adjustTypeId(adjustTypeIndex);
        }
        setChanged();
    }

    @Override
    protected int encode(ObjectArrayList<Instance> array, MemoryDataOutput mdo) {
        return ARRT.encodeArrayInstanceWithoutSize(array, mdo);
    }

    @Override
    protected void decode(ObjectArrayList<Instance> array, MemoryDataInput mdi) {
        ARRT.decodeArray(array, mdi, pool);
    }

    @Override
    public void setChanged() {
        typedDecoded = false;
        super.setChanged();
    }

    public void setTypedChanged() {
        typedChanged = true;
    }

    public void writeTyped() {
        encodeTyped();
    }

    public ObjectArrayList getTyped() {
        decodeTyped();
        return typedArray;
    }

    public void decodeTyped() {
        checkTyped();
        if (!typedDecoded) {
            encode();
            typedDecoded = true;
            in.setBytes(bytes());
            polyArrayCodec.decodeList(tempTypedArray, in);
            typedArray = tempTypedArray;
        }
    }

    public void encodeTyped() {
        checkTyped();
        if (typedChanged) {
            typedChanged = false;
            if (typedArray == null) {
                reset();
            } else {
                out.reset();
                polyArrayCodec.encodeList(typedArray, out);
                set(NULL_TYPE, out);
            }
            decoded = false;
        }
    }

    private void checkTyped() {
        if (polyArrayCodec == null) {
            throw new RuntimeException("Types are not supported");
        }
    }
}