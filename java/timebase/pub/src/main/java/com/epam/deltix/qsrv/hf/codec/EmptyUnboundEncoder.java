package com.epam.deltix.qsrv.hf.codec;

import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.FixedUnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.RecordClassInfo;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 * Date: Feb 24, 2010
 * @author BazylevD
 */
public final class EmptyUnboundEncoder implements FixedUnboundEncoder {
    private final RecordLayout layout;

    public EmptyUnboundEncoder(RecordLayout layout) {
        this.layout = layout;
    }

    @Override
    public void beginWrite(MemoryDataOutput out) {
    }

    @Override
    public void endWrite() {
    }

    @Override
    public RecordClassInfo getClassInfo() {
        return layout;
    }

    @Override
    public boolean nextField() {
        return false;
    }

    @Override
    public NonStaticFieldInfo getField() {
        return null;
    }

    @Override
    public void writeBoolean(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeChar(char value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeInt(int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeLong(long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeFloat(float value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeDouble(double value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeString(CharSequence value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setArrayLength(int len) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WritableValue nextWritableElement() {
        throw new UnsupportedOperationException();
    }

    @Override
    public UnboundEncoder getFieldEncoder(RecordClassDescriptor rcd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBinary(byte[] data, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeNull() {
        throw new UnsupportedOperationException();
    }
}
