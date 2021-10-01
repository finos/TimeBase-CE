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

import java.lang.reflect.InvocationTargetException;

/**
 * Generic interface having getter methods for all major unboxed types
 */
public interface ValueGetter {

    boolean getBoolean(Object obj) throws IllegalAccessException, InvocationTargetException;

    char getChar(Object obj) throws IllegalAccessException, InvocationTargetException;

    byte getByte(Object obj) throws IllegalAccessException, InvocationTargetException;

    short getShort(Object obj) throws IllegalAccessException, InvocationTargetException;

    int getInt(Object obj) throws IllegalAccessException, InvocationTargetException;

    long getLong(Object obj) throws IllegalAccessException, InvocationTargetException;

    float getFloat(Object obj) throws IllegalAccessException, InvocationTargetException;

    double getDouble(Object obj) throws IllegalAccessException, InvocationTargetException;

    Object get(Object obj) throws IllegalAccessException, InvocationTargetException;
}