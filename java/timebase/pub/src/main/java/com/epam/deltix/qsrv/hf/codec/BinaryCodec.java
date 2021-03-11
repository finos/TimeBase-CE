package com.epam.deltix.qsrv.hf.codec;

import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import rtmath.containers.BinaryArray;
import rtmath.containers.BinaryArrayHelper;
import rtmath.containers.interfaces.BinaryArrayReadOnly;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * User: BazylevD
 * Date: Dec 1, 2009
 * Time: 9:03:27 PM
 */
public class BinaryCodec {

    public static void writeNull(MemoryDataOutput out) {
        out.writeBoolean(true);
    }

    public static void write(byte[] data, int offset, int length, MemoryDataOutput out, int compressionLevel) {
        if (data == null)
            out.writeBoolean(true);
        else {
            // isNull, length, data
            out.writeBoolean(false);
            out.writePackedUnsignedInt(length);
            if (compressionLevel == 0)
                out.write(data, offset, length);
            else
                throw new UnsupportedOperationException("compressed BINARY is not supported yet.");
        }
    }

    public static void write(BinaryArrayReadOnly array, int offset, int length, MemoryDataOutput out, int compressionLevel) {
        if (array == null)
            out.writeBoolean(true);
        else {
            // isNull, length, data
            out.writeBoolean(false);
            out.writePackedUnsignedInt(length);
            out.ensureSize(length + out.getPosition());

            if (compressionLevel == 0) {
                array.getBytes(out.getBuffer(), offset, out.getPosition(), length);
                out.seek(out.getSize());
            }
            else
                throw new UnsupportedOperationException("compressed BINARY is not supported yet.");
        }
    }

    private MemoryDataInput in = null;
    private boolean isNull;
    private int length = -1;
    private int startPosition;
    private int dataPosition;

    public static void      skip (MemoryDataInput mdi) {
        if (mdi.readBoolean ())  // is null
            return;

        final int           length = mdi.readPackedUnsignedInt ();

        mdi.skipBytes (length);
    }

    public void readHeader (MemoryDataInput in) {        
        this.in = in;
        startPosition = in.getPosition();
        isNull = in.readBoolean();
        length = isNull ? 0 : in.readPackedUnsignedInt();
        dataPosition = in.getPosition();
        restorePosition();
    }

    public boolean isNull() {
        return isNull;
    }

    public int getLength() throws NullValueException {
        if (isNull)
            throw NullValueException.INSTANCE;
        else
            return length;
    }
    
    public int getDataPosition() throws NullValueException {
        if (isNull)
            throw NullValueException.INSTANCE;
        else
            return dataPosition;        
    }

    public int getDataOffset() throws NullValueException {
        if (isNull)
            throw NullValueException.INSTANCE;
        else
            return dataPosition + in.getStart();
    }

    public void getBinary(int offset, int length, OutputStream out, int compressionLevel) throws NullValueException {
        if (isNull)
            throw NullValueException.INSTANCE;
        else {
            if (compressionLevel == 0)
                try {
                    out.write(in.getBytes(), in.getStart() + dataPosition + offset, length);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            else {
                throw new UnsupportedOperationException("compressed BINARY is not supported yet.");
            }
        }
    }

    public void getBinary(int offset, int length, BinaryArray array, int compressionLevel) throws NullValueException {
        if (isNull)
            throw NullValueException.INSTANCE;
        else {
            if (compressionLevel == 0)
                array.append(in.getBytes(), in.getStart() + dataPosition + offset, length);
            else
                throw new UnsupportedOperationException("compressed BINARY is not supported yet.");
        }
    }

    public void getBinary(int srcOffset, int destOffset, int length, byte[] dest, int compressionLevel) throws NullValueException {
        if (isNull)
            throw NullValueException.INSTANCE;
        else {
            in.seek(dataPosition + srcOffset);
            if (compressionLevel == 0)
                in.readFully(dest, destOffset, length);
            else {
                throw new UnsupportedOperationException("compressed BINARY is not supported yet.");
            }

            restorePosition();
        }
    }

    public InputStream openBinary(int compressionLevel) {
        if (isNull)
            throw NullValueException.INSTANCE;
        else {
            in.seek(dataPosition);
            if (compressionLevel == 0)
                return new ByteArrayInputStream(in.getBytes(), in.getStart() + dataPosition, length);
            else {
                throw new UnsupportedOperationException("compressed BINARY is not supported yet.");
            }
        }
    }

    public void skip() {
        in.seek(dataPosition + length);
    }

    // Restore position to the beginnig of the field.
    // it is necessary, because UnboundDecoder.nextField implementation depends on it
    private void restorePosition() {
        in.seek(startPosition);
    }
}
