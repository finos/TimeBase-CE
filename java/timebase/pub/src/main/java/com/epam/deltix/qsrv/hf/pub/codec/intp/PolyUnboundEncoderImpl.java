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
