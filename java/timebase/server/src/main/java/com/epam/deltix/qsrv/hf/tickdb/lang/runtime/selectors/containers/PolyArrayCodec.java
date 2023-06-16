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
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.BoundExternalDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.PolyBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

public class PolyArrayCodec {

    private static final CodecFactory COMPILED_FACTORY = CodecFactory.newCompiledCachingFactory();
    private final Introspector introspector = Introspector.createEmptyMessageIntrospector();

    private final Class<?>[] classes;
    private final TypeLoader typeLoader;
    private final RecordClassDescriptor[] descriptors;

    private final MemoryDataOutput temp = new MemoryDataOutput();

    private PolyBoundEncoder encoder;
    private final BoundExternalDecoder[] decoders;

    private final ObjectPool<?>[] pool;

    private boolean isDescriptorsInitialized;

    public PolyArrayCodec(RecordClassDescriptor[] descriptors, Class<?>[] classes) {
        this.descriptors = descriptors;
        this.classes = classes;
        this.typeLoader = new TypeLoaderImpl(Thread.currentThread().getContextClassLoader());
        this.decoders = new BoundExternalDecoder[classes.length];
        this.pool = new ObjectPool[classes.length];

        for (int i = 0; i < classes.length; ++i) {
            Class<?> cls = classes[i];
            this.pool[i] = new ObjectPool<Object>() {
                public Object newItem() {
                    try {
                        return cls.getDeclaredConstructor().newInstance();
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
            };
        }
    }

    public void encodeList(ObjectArrayList list, MemoryDataOutput out) {
        out.reset();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); i++) {
            Object object = list.getObject(i);
            encoder().encode(object, temp);
            MessageSizeCodec.write(temp.getPosition(), out);
            out.write(temp.getBuffer(), 0, temp.getPosition());
            temp.reset();
        }
    }

    public void decodeList(ObjectArrayList list, MemoryDataInput in) {
        list.clear();
        resetPools();

        if (in.getLength() == 0) {
            return;
        }

        int length = MessageSizeCodec.read(in);
        for (int i = 0; i < length; i++) {
            int messageLength = MessageSizeCodec.read(in);
            if (messageLength > 0) {
                int typeId = in.readUnsignedByte();
                Object obj = borrowObject(typeId, cls(typeId));
                decoder(typeId).decode(in, obj);
                list.add(obj);
            } else {
                list.add(null);
            }
        }
    }

    private PolyBoundEncoder encoder() {
        if (encoder == null) {
            encoder = COMPILED_FACTORY.createPolyBoundEncoder(typeLoader, descriptors());
        }
        return encoder;
    }

    private BoundExternalDecoder decoder(int typeId) {
        if (decoders[typeId] == null) {
            decoders[typeId] = COMPILED_FACTORY.createFixedBoundDecoder(typeLoader, descriptor(typeId));
        }
        return decoders[typeId];
    }

    private RecordClassDescriptor descriptor(int typeId) {
        RecordClassDescriptor[] descriptors = descriptors();
        if (typeId < 0 || typeId >= descriptors.length) {
            throw new RuntimeException("Unknown typeId: " + typeId);
        }

        return descriptors[typeId];
    }

    private RecordClassDescriptor[] descriptors() {
        if (!isDescriptorsInitialized) {
            for (int i = 0; i < descriptors.length; i++) {
                if (descriptors[i] == null) {
                    try {
                        descriptors[i] = introspector.introspectMemberClass(PolyInstanceCodec.class.getSimpleName(), classes[i]);
                    } catch (Introspector.IntrospectionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            isDescriptorsInitialized = true;
        }

        return descriptors;
    }

    private <T> T borrowObject(int typeId, Class<T> cls) {
        ObjectPool<?> pool = pool(typeId);
        return cls.cast(pool.borrow());
    }

    private Class<?> cls(int typeId) {
        if (typeId < 0 || typeId >= pool.length) {
            throw new RuntimeException("Unknown typeId: " + typeId);
        }

        return classes[typeId];
    }

    private ObjectPool<?> pool(int typeId) {
        if (typeId < 0 || typeId >= pool.length) {
            throw new RuntimeException("Unknown typeId: " + typeId);
        }

        return pool[typeId];
    }

    private void resetPools() {
        for (int i = 0; i < pool.length; ++i) {
            pool[i].reset();
        }
    }

}