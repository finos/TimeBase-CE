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
package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.util.collections.SmallArrays;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.time.Interval;

import java.io.IOException;
import java.io.OutputStream;
import java.io.DataOutputStream;

/**
 * Base class for InstrumentMessages serialization to persistent data storage
 */
public class AbstractMessageWriter {
    protected int                               numTypes = 0;
    protected final Class <?> []                classes = new Class <?> [256];
    protected final RecordClassDescriptor []    types = new RecordClassDescriptor [256];
    protected final FixedBoundEncoder []        encoders = new FixedBoundEncoder [256];

    public AbstractMessageWriter() {
    }

    public RecordClassDescriptor[] getTypes () {
        RecordClassDescriptor []        ret = new RecordClassDescriptor [numTypes];

        System.arraycopy (types, 0, ret, 0, numTypes);

        return (ret);
    }

    protected int                     addNew (
        RecordClassDescriptor           type,
        Class <?>                       cls,
        FixedBoundEncoder encoder
    )
    {
        if (numTypes == 255)
            throw new IllegalStateException ("Too many type descriptors");

        final int           ret = numTypes++;

        types [ret] = type;
        classes [ret] = cls;
        encoders [ret] = encoder;

        return (ret);
    }

    protected void                  set(int index, RecordClassDescriptor rcd, Class <?> cls, FixedBoundEncoder encoder) {
        assert types[index] == rcd;

        classes[index] = cls;
        encoders[index] = encoder;
    }

    protected int                     getTypeIndex (RecordClassDescriptor type) {
        return SmallArrays.indexOf(type, types, numTypes);
    }

    protected int                     getTypeIndex(Class <?> cls) {
        return SmallArrays.indexOf(cls, classes, numTypes);
    }

    protected void                     encode (InstrumentMessage msg, MemoryDataOutput buffer)
            throws IOException
    {
        buffer.reset ();

        TimeCodec.writeTime (msg, buffer);
        buffer.writeByte (0); // instrument type
        IOUtil.writeUTF (msg.getSymbol(), buffer);

        if (msg instanceof RawMessage) {
            RawMessage          raw = (RawMessage) msg;
            int                 typeCode = getTypeIndex(raw.type);

            if (typeCode == -1)
                throw new IllegalArgumentException("ClassDescriptor " + raw.type + " is not mapped.");

            buffer.writeUnsignedByte (typeCode);
            raw.writeTo (buffer);
        } else {
            int                 typeCode = getTypeIndex(msg.getClass());

            if (typeCode == -1)
                throw new IllegalArgumentException("Class " + msg.getClass() + " is not mapped.");

            buffer.writeUnsignedByte (typeCode);
            encoders[typeCode].encode (msg, buffer);
        }       
    }

    protected static void writeHeader(OutputStream out, Interval periodicity, RecordClassDescriptor[] types)
            throws IOException
    {
        out.write (Protocol.MAGIC);
        out.write (Protocol.VERSION);
        Protocol.writeTypes (new DataOutputStream(out), types);

        DataOutputStream dout = new DataOutputStream(out);
        dout.writeBoolean(periodicity != null);
        if (periodicity != null)
            dout.writeUTF(periodicity.toString());
    }

//    public static void writeHeader(OutputStream out, RecordClassDescriptor[] types) throws IOException {
//        out.write (Protocol.MAGIC);
//        out.write (Protocol.VERSION);
//        Protocol.writeTypes (new DataOutputStream(out), types);
//    }
}
