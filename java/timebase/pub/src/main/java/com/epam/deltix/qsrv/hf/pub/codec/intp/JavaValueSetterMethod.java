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

import com.epam.deltix.qsrv.hf.pub.codec.FieldLayout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Implementation, which uses Java-reflection
 */
public class JavaValueSetterMethod implements ValueSetter {

    private final Method setter;
    private final Method getter;
    private final Method nullifier;

    public JavaValueSetterMethod (FieldLayout layout) {
        this.setter = layout.getSetter();
        this.getter = layout.getGetter();
        this.nullifier = layout.getNullifier();
    }

    public boolean hasNullifier () {
        return this.nullifier != null;
    }

    public void nullifyValue (Object object) throws InvocationTargetException, IllegalAccessException {
        this.nullifier.invoke(object);
    }

    public String getSetterName () {
        return this.setter.getName();
    }

    @Override
    public void setBoolean (Object obj, boolean value) throws IllegalAccessException, InvocationTargetException {
        this.setter.invoke(obj, value);
    }

    @Override
    public void setChar (Object obj, char value) throws IllegalAccessException, InvocationTargetException {
        this.setter.invoke(obj, value);
    }

    @Override
    public void setByte (Object obj, byte value) throws IllegalAccessException, InvocationTargetException {
        this.setter.invoke(obj, value);
    }

    @Override
    public void setShort (Object obj, short value) throws IllegalAccessException, InvocationTargetException {
        this.setter.invoke(obj, value);
    }

    @Override
    public void setInt (Object obj, int value) throws IllegalAccessException, InvocationTargetException {
        this.setter.invoke(obj, value);
    }

    @Override
    public void setLong (Object obj, long value) throws IllegalAccessException, InvocationTargetException {
        this.setter.invoke(obj, value);
    }

    @Override
    public void setFloat (Object obj, float value) throws IllegalAccessException, InvocationTargetException {
        this.setter.invoke(obj, value);
    }

    @Override
    public void setDouble (Object obj, double value) throws IllegalAccessException, InvocationTargetException {
        this.setter.invoke(obj, value);
    }

    @Override
    public void set (Object obj, Object value) throws IllegalAccessException, InvocationTargetException {
        this.setter.invoke(obj, value);
    }

    @Override
    public Object get (Object obj) throws IllegalAccessException, InvocationTargetException {
        return this.getter.invoke(obj);
    }
}