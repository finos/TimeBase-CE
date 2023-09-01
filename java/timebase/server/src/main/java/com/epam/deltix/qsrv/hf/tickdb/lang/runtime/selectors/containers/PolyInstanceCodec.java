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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.BoundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

public class PolyInstanceCodec {

    private static final CodecFactory COMPILED_FACTORY = CodecFactory.newCompiledCachingFactory();

    private final Class<?>[] classes;
    private final RecordTypeMap<Class<?>> classToTypeId;
    private final TypeLoader typeLoader;
    private final BoundDecoder[] decoders;
    private final FixedBoundEncoder[] encoders;
    private final Introspector introspector = Introspector.createEmptyMessageIntrospector();

    private RecordClassDescriptor[] descriptors;

    public PolyInstanceCodec(Class<?> ... classes) {
        this.classes = classes;
        this.typeLoader = new TypeLoaderImpl(Thread.currentThread().getContextClassLoader());
        this.classToTypeId = new RecordTypeMap<>(classes);
        this.decoders = new BoundDecoder[classes.length];
        this.encoders = new FixedBoundEncoder[classes.length];
    }

    public Object decode(int typeId, MemoryDataInput in) {
        return decoder(typeId).decode(in);
    }

    @SuppressWarnings("rawtypes")
    public void decodeList(ObjectArrayList list, MemoryDataInput in) {
        list.clear();
        int length = MessageSizeCodec.read(in);
        for (int i = 0; i < length; i++) {
            int typeId = in.readByte();
            list.add(decode(typeId, in));
        }
    }

    public int encode(Object object, MemoryDataOutput out) {
        int typeId = classToTypeId.getCode(object.getClass());
        encoder(typeId).encode(object, out);
        return typeId;
    }

    @SuppressWarnings("rawtypes")
    public void encodeList(ObjectArrayList list, MemoryDataOutput out) {
        out.reset();
        MessageSizeCodec.write(list.size(), out);
        for (int i = 0; i < list.size(); i++) {
            Object object = list.getObject(i);
            int typeId = classToTypeId.getCode(object.getClass());
            out.writeByte(typeId);
            encoder(typeId).encode(object, out);
        }
    }

    private BoundDecoder decoder(int typeId) {
        BoundDecoder decoder = decoders[typeId];
        if (decoder == null) {
            decoders[typeId] = decoder = createDecoder(typeId);
        }

        return decoder;
    }

    private BoundDecoder createDecoder(int typeId) {
        return COMPILED_FACTORY.createFixedBoundDecoder(typeLoader, descriptors()[typeId]);
    }

    private FixedBoundEncoder encoder(int typeId) {
        FixedBoundEncoder encoder = encoders[typeId];
        if (encoder == null) {
            encoders[typeId] = encoder = createEncoder(typeId);
        }

        return encoder;
    }

    private FixedBoundEncoder createEncoder(int typeId) {
        return COMPILED_FACTORY.createFixedBoundEncoder(typeLoader, descriptors()[typeId]);
    }

    private RecordClassDescriptor[] descriptors() {
        if (descriptors == null) {
            descriptors = new RecordClassDescriptor[classes.length];
            for (int i = 0; i < classes.length; i++) {
                try {
                    descriptors[i] = introspector.introspectMemberClass(PolyInstanceCodec.class.getSimpleName(), classes[i]);
                } catch (Introspector.IntrospectionException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return descriptors;
    }

}