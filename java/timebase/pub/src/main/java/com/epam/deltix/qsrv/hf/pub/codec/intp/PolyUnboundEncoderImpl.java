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

import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public class PolyUnboundEncoderImpl implements PolyUnboundEncoder {
    private final FixedUnboundEncoder []                encoders;
    private FixedUnboundEncoder                         currentEncoder;
    private final RecordTypeMap<RecordClassDescriptor>  typeMap;
    
    public PolyUnboundEncoderImpl (FixedUnboundEncoder [] encoders) {
        this.encoders = encoders; 
        
        int                         num = encoders.length;
        RecordClassDescriptor []    classes = new RecordClassDescriptor [num];
        
        for (int ii = 0; ii < num; ii++) 
            classes [ii] = encoders [ii].getClassInfo ().getDescriptor ();
        
        typeMap = new RecordTypeMap<RecordClassDescriptor> (classes);
    }

    public void                 beginWrite (
        RecordClassDescriptor       rcd, 
        MemoryDataOutput            out
    )
    {
        int                     code = typeMap.getCode (rcd);
        
        out.writeUnsignedByte (code);
        
        currentEncoder = encoders [code];
        currentEncoder.beginWrite (out);
    }

    @Override
    public void endWrite() {
        currentEncoder.endWrite();
    }

    public void writeNull() {
        currentEncoder.writeNull();
    }

    public void                 writeBinary(byte[] data, int offset, int length) {
        currentEncoder.writeBinary (data, offset, length);
    }

    public void                 writeString (CharSequence value) {
        currentEncoder.writeString (value);
    }

    public void                 writeLong (long value) {
        currentEncoder.writeLong (value);
    }

    public void                 writeInt (int value) {
        currentEncoder.writeInt (value);
    }

    public void                 writeFloat (float value) {
        currentEncoder.writeFloat (value);
    }

    public void                 writeDouble (double value) {
        currentEncoder.writeDouble (value);
    }

    public void writeChar(char value) {
        currentEncoder.writeChar (value);
    }

    public void                 writeBoolean (boolean value) {
        currentEncoder.writeBoolean (value);
    }

    @Override
    public void setArrayLength(int len) {
        currentEncoder.setArrayLength(len);
    }

    @Override
    public WritableValue nextWritableElement() {
        return currentEncoder.nextWritableElement();
    }

    @Override
    public UnboundEncoder getFieldEncoder(RecordClassDescriptor rcd) {
        return currentEncoder.getFieldEncoder(rcd);
    }

    public boolean              nextField () {
        return currentEncoder.nextField ();
    }

    public NonStaticFieldInfo   getField () {
        return currentEncoder.getField ();
    }         
}