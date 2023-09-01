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
package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.codec.BinaryCodec;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.lang.Dump;
import com.epam.deltix.containers.BinaryArray;
import com.epam.deltix.containers.interfaces.BinaryArrayReadOnly;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * User: BazylevD
 * Date: Dec 1, 2009
 */
class BinaryFieldDecoder extends FieldDecoder {
    private final BinaryCodec codec;
    private final int compressionLevel;


    BinaryFieldDecoder (NonStaticFieldLayout f, int compressionLevel) {
        super (f);
        codec = new BinaryCodec();
        this.compressionLevel = compressionLevel;
    }

    @Override
    void skip(DecodingContext ctxt) {
        codec.readHeader(ctxt.in);
        codec.skip();
    }

    @Override
    final protected void copy(DecodingContext ctxt, Object obj) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final Class<?> fc = this.fieldType;

        if (fc == ByteArrayList.class) {
            try {
                final int len = getBinaryLength(ctxt);
                ByteArrayList data = (ByteArrayList) setter.get(obj);
                if (data == null)
                    data = new ByteArrayList(len);
                data.setSizeUnsafe(len);
                getBinary(data.getInternalBuffer(), 0, 0, len, ctxt);
                setter.set(obj, data);
            } catch (NullValueException e) {
                assert isNullable : getNotNullableMsg();
                setter.set(obj, null);
            }
        } else if (fc == byte[].class) {
            try {
                final int len = getBinaryLength(ctxt);
                byte[] data = (byte[]) setter.get(obj);
                if (data == null || data.length != len) {
                    data = new byte[len];
                    setter.set(obj, data);
                }
                getBinary(data, 0, 0, len, ctxt);
            } catch (NullValueException e) {
                assert isNullable : getNotNullableMsg();
                setter.set(obj, null);
            }
        }  else if (fc.isAssignableFrom(BinaryArrayReadOnly.class)) {
            try {
                final int len = getBinaryLength(ctxt);
                //BinaryArrayReadOnly data = (BinaryArrayReadOnly) setter.get(obj);

                BinaryArray array = new BinaryArray();
                setter.set(obj, array);
                codec.readHeader(ctxt.in);
                codec.getBinary(0, len, array, compressionLevel);

            } catch (NullValueException e) {
                assert isNullable : getNotNullableMsg();
                setter.set(obj, null);
            }
        }
        else
            throw new IllegalArgumentException("Type " + fc + " is not supported.");

        skip(ctxt);
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        setter.set(obj, null);
    }

    @Override
    String getString(DecodingContext ctx) {
        int length = getBinaryLength(ctx);

        if (!codec.isNull()) {
            String value = Dump.dump(ctx.in.getBytes(), codec.getDataOffset(), Math.min(length, 64)).toString();
            codec.skip();
            return value;
        }

        return null;
    }

    @Override
    int compare(DecodingContext ctxt1, DecodingContext ctxt2) {
        //TODO: implement it ???
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNull(DecodingContext ctxt) {
        codec.readHeader(ctxt.in);
        return codec.isNull();
    }

    @Override
    int getBinaryLength(DecodingContext ctxt) throws NullValueException {
        codec.readHeader(ctxt.in);
        return codec.getLength();
    }

    @Override
    void getBinary(int offset, int length, OutputStream out, DecodingContext ctxt) throws NullValueException {
        codec.readHeader(ctxt.in);
        codec.getBinary(offset, length, out, compressionLevel);
    }

    @Override
    void getBinary(byte[] data, int srcOffset, int destOffset, int length, DecodingContext ctxt) throws NullValueException {
        codec.readHeader(ctxt.in);
        codec.getBinary(srcOffset, destOffset, length, data, compressionLevel);
    }

    @Override
    InputStream openBinary(DecodingContext ctxt) {
        codec.readHeader(ctxt.in);
        return codec.openBinary(compressionLevel);
    }
}