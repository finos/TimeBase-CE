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
package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public class ClassFieldEncoder extends FieldEncoder implements ContainerEncoder {
    // unbound API implementation
    //private final MemoryDataOutput     out = new MemoryDataOutput();

    private final UnboundContainerEncoder   unboundEncoder;
    private final BoundContainerEncoder     boundEncoder;
    private boolean                         isNull = false;

    ClassFieldEncoder(TypeLoader loader, NonStaticFieldLayout f) {
        super(f);

        RecordClassDescriptor[] types = ((ClassDataType) f.getType()).getDescriptors();

        if (types.length > ClassFieldDecoder.NULL_CODE)
            throw new IllegalArgumentException(
                "Too many classes: " + types.length + " (max 255)"
            );

        boundEncoder = loader != null ? new BoundContainerEncoder(loader, types) : null;
        unboundEncoder = loader == null ? new UnboundContainerEncoder(types) : null;
    }

    @Override
    public void                 beginWrite(MemoryDataOutput out) {
        unboundEncoder.beginWrite(out);
        isNull = false;
    }

    @Override
    public void                 endWrite() {
        // write content of encoder only if it's not null

        if (!isNull)
            unboundEncoder.endWrite();
    }

    @Override
    final protected void        copy(Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        writeObject(getter.get(obj), ctxt);
    }

    public void                 writeObject(Object value, EncodingContext ctx) {
        if (!isNullable && value == null)
            throwNotNullableException();

        boundEncoder.writeObject(value, ctx);
    }

    @Override
    void                        writeNull(EncodingContext ctx) {
        if (isNullable) {
            MessageSizeCodec.write(0, ctx.out);
            isNull = true;
        } else {
            throwNotNullableException();
        }
    }
    @Override
    public UnboundEncoder       getFieldEncoder(RecordClassDescriptor rcd) {
        return unboundEncoder.getEncoder(rcd);
    }

    @Override
    void                        setString(CharSequence value, EncodingContext ctxt) {
        throw new UnsupportedOperationException("Not supported for Class field");
    }

    @Override
    protected boolean           isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        return getter.get(message) == null;
    }
}