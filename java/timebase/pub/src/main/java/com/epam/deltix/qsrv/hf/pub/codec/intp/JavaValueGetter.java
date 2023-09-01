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

import java.lang.reflect.Field;

/**
 * Implementation, which uses Java-reflection
 */
public class JavaValueGetter implements ValueGetter {
    private final Field f;

    public JavaValueGetter(Field f) {
        this.f = f;
    }

    @Override
    public boolean getBoolean(Object obj) throws IllegalAccessException {
        return f.getBoolean(obj);
    }

    @Override
    public char getChar(Object obj) throws IllegalAccessException {
        return f.getChar(obj);
    }

    @Override
    public byte getByte(Object obj) throws IllegalAccessException {
        return f.getByte(obj);
    }

    @Override
    public short getShort(Object obj) throws IllegalAccessException {
        return f.getShort(obj);
    }

    @Override
    public int getInt(Object obj) throws IllegalAccessException {
        return f.getInt(obj);
    }

    @Override
    public long getLong(Object obj) throws IllegalAccessException {
        return f.getLong(obj);
    }

    @Override
    public float getFloat(Object obj) throws IllegalAccessException {
        return f.getFloat(obj);
    }

    @Override
    public double getDouble(Object obj) throws IllegalAccessException {
        return f.getDouble(obj);
    }

    @Override
    public Object get(Object obj) throws IllegalAccessException {
        return f.get(obj);
    }
}