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
package com.epam.deltix.qsrv.hf.pub;

import java.io.OutputStream;
import java.io.InputStream;

import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;

/**
 *  Read access to a value.
 */
public interface ReadableValue {
    /**
     * Always returns <code>false</code> for non-nullable field.
     */
    boolean isNull();

    boolean getBoolean() throws NullValueException;

    char getChar() throws NullValueException;

    byte getByte() throws NullValueException;

    short getShort() throws NullValueException;

    int getInt() throws NullValueException;

    long getLong() throws NullValueException;

    float getFloat() throws NullValueException;

    double getDouble() throws NullValueException;

    String getString() throws NullValueException;

    int getArrayLength() throws NullValueException;

    ReadableValue nextReadableElement() throws NullValueException;

    /**
     * Used to read nested objects
     */
    UnboundDecoder getFieldDecoder() throws NullValueException;

    int getBinaryLength() throws NullValueException;

    void getBinary(int offset, int length, OutputStream out) throws NullValueException;

    void getBinary(int srcOffset, int length, byte[] dest, int destOffset) throws NullValueException;

    InputStream openBinary() throws NullValueException;
}