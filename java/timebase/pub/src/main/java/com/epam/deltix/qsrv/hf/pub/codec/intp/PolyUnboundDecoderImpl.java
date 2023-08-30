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

import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.RecordClassInfo;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.ValidationError;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.io.OutputStream;
import java.io.InputStream;

public class PolyUnboundDecoderImpl implements UnboundDecoder {
    private final UnboundDecoder[]    decoders;
    private UnboundDecoder currentDecoder;

    public PolyUnboundDecoderImpl(UnboundDecoder[] decoders) {
        this.decoders = decoders;
    }

    public void                 beginRead (MemoryDataInput in) {
        int             code = in.readUnsignedByte ();

        currentDecoder = decoders [code];
        currentDecoder.beginRead (in);
    }

     public boolean              nextField () {
        return currentDecoder.nextField ();
    }

    public boolean              previousField() {
        return currentDecoder.previousField();
    }

    public boolean              seekField(int index) {
        return currentDecoder.seekField(index);
    }

    public RecordClassInfo      getClassInfo () {
        return currentDecoder.getClassInfo ();
    }
    
    public boolean              isNull() {
        return currentDecoder.isNull();
    }

    public int                  compareAll (MemoryDataInput in1, MemoryDataInput in2) {
        int             code1 = in1.readUnsignedByte ();
        int             code2 = in2.readUnsignedByte ();
        int             dif = code1 - code2;

        if (dif != 0)
            return (dif);

        return (decoders [code1].compareAll (in1, in2));
    }

    public int                  comparePrimaryKeys (MemoryDataInput in1, MemoryDataInput in2) {
        int             code1 = in1.readUnsignedByte ();
        int             code2 = in2.readUnsignedByte ();
        int             dif = code1 - code2;

        if (dif != 0)
            return (dif);

        return (decoders [code1].comparePrimaryKeys (in1, in2));
    } 

    public String               getString () {
        return currentDecoder.getString ();
    }

    public long                 getLong () {
        return currentDecoder.getLong ();
    }

    public int                  getInt () {
        return currentDecoder.getInt ();
    }

    public float                getFloat () {
        return currentDecoder.getFloat ();
    }

    public NonStaticFieldInfo   getField () {
        return currentDecoder.getField ();
    }

    public double               getDouble () {
        return currentDecoder.getDouble ();
    }

    public char getChar() {
        return currentDecoder.getChar();
    }

    @Override
    public byte getByte() throws NullValueException {
        return currentDecoder.getByte();
    }

    @Override
    public short getShort() throws NullValueException {
        return currentDecoder.getShort();
    }

    public boolean              getBoolean () {
        return currentDecoder.getBoolean ();
    }

    @Override
    public int getArrayLength() throws NullValueException {
        return currentDecoder.getArrayLength();
    }

    @Override
    public ReadableValue nextReadableElement() throws NullValueException {
        return currentDecoder.nextReadableElement();
    }

    @Override
    public UnboundDecoder getFieldDecoder() throws NullValueException {
        return currentDecoder.getFieldDecoder();
    }

    @Override
    public int                  getBinaryLength() throws NullValueException {
        return currentDecoder.getBinaryLength();
    }

    @Override
    public void                 getBinary(int offset, int length, OutputStream out) throws NullValueException {
        currentDecoder.getBinary(offset, length, out);
    }

    @Override
    public void                 getBinary(int srcOffset, int length, byte [] dest, int destOffset) throws NullValueException {
        currentDecoder.getBinary(srcOffset, length, dest, destOffset);
    }

    @Override
    public InputStream openBinary() {
        return currentDecoder.openBinary();
    }

    @Override
    public ValidationError validate () {
        throw new UnsupportedOperationException();
    }
}