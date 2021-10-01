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
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.BoundDecoder;
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
    private BoundDecoder decoder;

    public PolyArrayCodec(Class<?> ... classes) {
        this.classes = classes;
        this.typeLoader = new TypeLoaderImpl(Thread.currentThread().getContextClassLoader());
        this.descriptors = new RecordClassDescriptor[classes.length];
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
        int length = MessageSizeCodec.read(in);
        for (int i = 0; i < length; i++) {
            list.add(decoder().decode(in));
        }
    }

    private PolyBoundEncoder encoder() {
        if (encoder == null) {
            encoder = COMPILED_FACTORY.createPolyBoundEncoder(typeLoader, descriptors());
        }
        return encoder;
    }

    private BoundDecoder decoder() {
        if (decoder == null) {
            decoder = COMPILED_FACTORY.createPolyBoundDecoder(typeLoader, descriptors());
        }
        return decoder;
    }

    private RecordClassDescriptor[] descriptors() {
        if (descriptors[0] == null) {
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