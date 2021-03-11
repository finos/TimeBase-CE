package com.epam.deltix.qsrv.hf.tickdb.schema.encoders;

import com.epam.deltix.qsrv.hf.pub.WritableValue;

public interface MixedWritableValue extends WritableValue {

    public void                 writeBoolean(int value);
    public void                 writeBoolean(long value);
    public void                 writeBoolean(float value);
    public void                 writeBoolean(double value);

    public void                 writeInt (long value);
    public void                 writeInt (double value);

    public void                 writeLong (float value);
    public void                 writeLong (double value);

    public void                 writeFloat (double value);
    
    public void                 writeEnum (CharSequence value);

    public void                 writeDefault();

    public  MixedWritableValue  clone(WritableValue out);
}