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

import com.epam.deltix.qsrv.hf.codec.BinaryCodec;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.BinaryDataType;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.containers.interfaces.BinaryArrayReadOnly;

import java.lang.reflect.InvocationTargetException;

/**
 * User: BazylevD
 * Date: Dec 1, 2009
 */
class BinaryFieldEncoder extends FieldEncoder {
    private final int compressionLevel;
    private final int maxSize;

    BinaryFieldEncoder(NonStaticFieldLayout f, int maxSize, int compressionLevel) {
        super(f);
        this.maxSize = maxSize;
        this.compressionLevel = compressionLevel;
    }

    @Override
    final protected void copy(Object obj, EncodingContext ctxt) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final Class<?> fc = this.fieldType;
        if (fc == ByteArrayList.class) {
            ByteArrayList data = (ByteArrayList) getter.get(obj);
            if (data == null)
                writeNull(ctxt);
            else {
                setBinary(data.getInternalBuffer(), 0, data.size(), ctxt);
            }
        } else if (fc == byte[].class) {
            final byte[] data = (byte[]) getter.get(obj);
            if (data == null)
                writeNull(ctxt);
            else {
                setBinary(data, 0, data.length, ctxt);
            }
        } else if (fc.isAssignableFrom(BinaryArrayReadOnly.class)) {
            BinaryArrayReadOnly data = (BinaryArrayReadOnly) getter.get(obj);
            if (data == null)
                writeNull(ctxt);
            else {
                checkLimit(data.size());
                BinaryCodec.write(data, 0, data.size(), ctxt.out, compressionLevel);
            }
        }
        else
            throw new IllegalArgumentException("Type " + fc + " is not supported.");
    }

    @Override
    void writeNull(EncodingContext ctxt) {
        if (!isNullable)
            throwNotNullableException();
        else
            BinaryCodec.writeNull(ctxt.out);
    }

    @Override
    void setString(CharSequence value, EncodingContext ctxt) {
        throw new UnsupportedOperationException();
    }

    private void        checkLimit(int length) {
        if (length > maxSize && maxSize != BinaryDataType.UNLIMITED_SIZE)
            throw new IllegalArgumentException(String.format("An attempt to write more data then specified by maxSize: %d > %d", length, maxSize));
    }

    @Override
    void setBinary(byte[] data, int offset, int length, EncodingContext ctxt) {
        checkLimit(length);

        BinaryCodec.write(data, offset, length, ctxt.out, compressionLevel);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        return getter.get(message) == null;
    }
}
