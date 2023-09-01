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

import java.lang.reflect.InvocationTargetException;

/**
 * Generic interface having setter methods for all major unboxed types
 */
public interface ValueSetter {

    void setBoolean(Object obj, boolean value) throws IllegalAccessException, InvocationTargetException;

    void setChar(Object obj, char value) throws IllegalAccessException, InvocationTargetException;

    void setByte(Object obj, byte value) throws IllegalAccessException, InvocationTargetException;

    void setShort(Object obj, short value) throws IllegalAccessException, InvocationTargetException;

    void setInt(Object obj, int value) throws IllegalAccessException, InvocationTargetException;

    void setLong(Object obj, long value) throws IllegalAccessException, InvocationTargetException;

    void setFloat(Object obj, float value) throws IllegalAccessException, InvocationTargetException;

    void setDouble(Object obj, double value) throws IllegalAccessException, InvocationTargetException;

    void set(Object obj, Object value) throws IllegalAccessException, InvocationTargetException;

    Object get(Object ogj) throws IllegalAccessException, InvocationTargetException;
}