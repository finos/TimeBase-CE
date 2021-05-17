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


import com.epam.deltix.qsrv.hf.pub.codec.FieldLayout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Implementation, which uses Java-reflection
 */
public class JavaValueGetterMethod implements ValueGetter {

    private final Method getter;
    private final Method haser;

    public JavaValueGetterMethod (FieldLayout layout) {
        this.getter = layout.getGetter();
        this.haser = layout.getHaser();
    }

    public boolean hasHaser () {
        return this.haser != null;
    }

    public boolean hasValue (Object obj) throws IllegalAccessException, InvocationTargetException {
        return (boolean) haser.invoke(obj);
    }

    public String getGetterName () {
        return this.getter.getName();
    }

    @Override
    public boolean getBoolean (Object obj) throws IllegalAccessException, InvocationTargetException {
        return (boolean) getter.invoke(obj);
    }

    @Override
    public char getChar (Object obj) throws IllegalAccessException, InvocationTargetException {
        return (char) getter.invoke(obj);
    }

    @Override
    public byte getByte (Object obj) throws IllegalAccessException, InvocationTargetException {
        return (byte) getter.invoke(obj);
    }

    @Override
    public short getShort (Object obj) throws IllegalAccessException, InvocationTargetException {
        return (short) getter.invoke(obj);
    }

    @Override
    public int getInt (Object obj) throws IllegalAccessException, InvocationTargetException {
        return (int) getter.invoke(obj);
    }

    @Override
    public long getLong (Object obj) throws IllegalAccessException, InvocationTargetException {
        return (long) getter.invoke(obj);
    }

    @Override
    public float getFloat (Object obj) throws IllegalAccessException, InvocationTargetException {
        return (float) getter.invoke(obj);
    }

    @Override
    public double getDouble (Object obj) throws IllegalAccessException, InvocationTargetException {
        return (double) getter.invoke(obj);
    }

    @Override
    public Object get (Object obj) throws IllegalAccessException, InvocationTargetException {
        return getter.invoke(obj);
    }
}
