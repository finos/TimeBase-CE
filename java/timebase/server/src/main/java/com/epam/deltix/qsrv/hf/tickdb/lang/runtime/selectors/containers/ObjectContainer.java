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

import com.epam.deltix.qsrv.hf.pub.codec.BoundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.Instance;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class ObjectContainer<T> extends Instance {

    protected static final CodecFactory COMPILED_FACTORY = CodecFactory.newCompiledCachingFactory();
    protected static final Introspector INTROSPECTOR = Introspector.createEmptyMessageIntrospector();

    protected final T bufferMessage;
    protected final RecordClassDescriptor rcd;
    protected final BoundDecoder decoder;
    protected final FixedBoundEncoder encoder;
    protected final MemoryDataInput in = new MemoryDataInput();
    protected final MemoryDataOutput out = new MemoryDataOutput();

    protected T message;
    protected boolean changed = false;
    protected boolean decoded = true;
    protected int typeIdPosition = -1;

    public ObjectContainer(@Nonnull Supplier<T> supplier) {
        this.bufferMessage = supplier.get();
        try {
            this.rcd = INTROSPECTOR.introspectRecordClass(bufferMessage.getClass());
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }
        this.decoder = COMPILED_FACTORY.createFixedBoundDecoder(x -> bufferMessage.getClass(), rcd);
        this.encoder = COMPILED_FACTORY.createFixedBoundEncoder(x -> bufferMessage.getClass(), rcd);
    }

    @Override
    public void decode(MemoryDataInput mdi) {
        decoded = false;
        super.decode(mdi);
    }

    @Override
    public void encode(MemoryDataOutput out) {
        encode();
        super.encode(out);
    }

    public T get() {
        decode();
        return message;
    }

    public void set(T object) {
        message = object;
        setChanged();
    }

    public void setNull() {
        set((Instance) null);
    }

    public void setChanged() {
        changed = true;
    }

    public void setInstance() {
        if (message == null) {
            message = bufferMessage;
            changed = true;
        }
    }

    @Override
    public byte[] bytes() {
        encode();
        return super.bytes();
    }

    @Override
    public int length() {
        encode();
        return super.length();
    }

    public void decode() {
        if (!decoded) {
            decoded = true;
            in.setBytes(bytes(), offset(), length());
            message = bufferMessage;
            decoder.decode(in, message);
        }
    }

    protected void encode() {
        if (changed) {
            changed = false;
            out.reset();
            encoder.encode(message, out);
            set(0, out);
        }
    }

    @Override
    public boolean isNull() {
        return message == null && super.isNull();
    }
}