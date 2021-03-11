package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.qsrv.hf.tickdb.pub.query.TypedMessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.codec.FixedExternalDecoder;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.time.Interval;

import java.io.*;
import java.util.Arrays;

/**
 * Base class for reading InstrumentMessage from persistent data storage
 */

public class AbstractMessageReader implements TypedMessageSource {
//    protected static final InstrumentType []      ITYPE_TYPES =
//            InstrumentType.values ();
//
    protected FixedExternalDecoder[]        decoders;
    protected InstrumentMessage []          messages;
    protected  byte []                      bytes = new byte [4096];
    protected final MemoryDataInput         buffer = new MemoryDataInput ();

    protected final StringBuilder           symbol = new StringBuilder ();
    protected RecordClassDescriptor []      types;
    protected InstrumentMessage             curMsg = null;
    protected RawMessage                    rawMsg;
    protected int                           curTypeCode;    

    public InstrumentMessage getMessage () {
        return (curMsg);
    }
   
    public static byte readVersion(InputStream in)
            throws IOException
    {
        byte[] trailer = new byte[4];
        if (in.read(trailer) == 4 && Arrays.equals(Protocol.MAGIC, trailer))
            return (byte)in.read();

        return 0;
    }

    public static MessageFileHeader readHeader(InputStream in)
            throws IOException
    {
        byte v = readVersion(in);
        if (v != Protocol.VERSION && v < 15)
            throw new IllegalArgumentException("File version '" + v + "' is not compatible with " + Protocol.VERSION);

        if (v > Protocol.VERSION) // version from future
            throw new IllegalArgumentException("File version '" + v + "' is not compatible with " + Protocol.VERSION);
        
        // read type descriptors from stream
        MessageFileHeader header = Protocol.readTypes(new DataInputStream(in), v);
        
        if (v > 15) {
            DataInputStream dout = new DataInputStream(in);
            if (dout.readBoolean())
                header.periodicity = Interval.valueOf(dout.readUTF());
        }
        
        return header;
    }

    protected void                          checkBuffer(int length) {
        if (bytes.length < length)
            bytes = new byte[length * 2];
    }

    protected InstrumentMessage             decode(MemoryDataInput buffer)
            throws IOException
    {
        final long nanos = TimeCodec.readNanoTime(buffer);
        int code = buffer.readUnsignedByte(); // old instrument type

        symbol.setLength (0);
        IOUtil.readUTF (buffer, symbol);

        curTypeCode = buffer.readUnsignedByte ();

        if (decoders == null) {
            rawMsg.type = types [curTypeCode];
            rawMsg.data = buffer.getBytes();
            rawMsg.offset = buffer.getCurrentOffset ();
            rawMsg.length = buffer.getAvail ();
            rawMsg.setNanoTime(nanos);
            return rawMsg;
        }
        else {
            curMsg = messages [curTypeCode];
            curMsg.setNanoTime(nanos);
            decoders [curTypeCode].decode (buffer, curMsg);
            return curMsg;
        }
    }

    public RecordClassDescriptor[]  getTypes() {
        return types;
    }    

    @Override
    public int                      getCurrentTypeIndex() {
        return curTypeCode;
    }

    @Override
    public RecordClassDescriptor    getCurrentType() {
        return getTypes()[curTypeCode];
    }
}
