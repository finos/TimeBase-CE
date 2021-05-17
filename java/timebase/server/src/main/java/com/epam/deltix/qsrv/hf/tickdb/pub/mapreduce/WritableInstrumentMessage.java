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
package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.impl.SimpleMessageDecoder;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.JobContext;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 */
public class WritableInstrumentMessage implements WritableMessage<InstrumentMessage>, WritableComparable<InstrumentMessage> {
    private String                      symbol;
    private long                        nanos;
    private MemoryDataOutput            mdo = new MemoryDataOutput();
    private MemoryDataInput             mdi = new MemoryDataInput();

    private int                         code = -1;
    private RecordClassDescriptor[]     types;
    private RecordTypeMap<Class>        typeMap;
    private FixedBoundEncoder[]         encoders;
    private SimpleMessageDecoder[]      decoders;

    public                      WritableInstrumentMessage() {
    }

    @Override
    public void                 set(InstrumentMessage message, JobContext context) throws IOException {
        if (types == null)
            initTypes(context);

        code = typeMap.getCode(message.getClass());
        symbol = message.getSymbol().toString();
        nanos = message.getNanoTime();

        mdo.reset();
        encoders[code].encode(message, mdo);
    }

    @Override
    public InstrumentMessage    get(JobContext context) throws IOException {
        if (types == null)
            initTypes(context);

        InstrumentMessage message = decoders[code].decode(mdi, mdi.getLength());
        message.setSymbol(symbol);
        message.setNanoTime(nanos);
        return message;
    }

    public void                 write(DataOutput dataOutput) throws IOException {
        WritableUtils.writeVInt(dataOutput, code);
        WritableUtils.writeString(dataOutput, symbol);
        WritableUtils.writeVLong(dataOutput, nanos);
        WritableUtils.writeCompressedByteArray(dataOutput, mdo.getBuffer());
    }

    public void                 readFields(DataInput dataInput) throws IOException {
        code = WritableUtils.readVInt(dataInput);
        symbol = WritableUtils.readString(dataInput);
        nanos = WritableUtils.readVLong(dataInput);
        mdi.setBytes(WritableUtils.readCompressedByteArray(dataInput));
    }

    private void                initTypes(JobContext context) throws IOException {
        types = TDBProtocol.readClassSet(context.getConfiguration().get(MAPPER_OUTPUT_TYPE)).getContentClasses();

        this.encoders = new FixedBoundEncoder[types.length];
        this.decoders = new SimpleMessageDecoder[types.length];
        final Class<?>[] classes = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            encoders[i] = CodecFactory.COMPILED.createFixedBoundEncoder(TypeLoaderImpl.DEFAULT_INSTANCE, types[i]);
            decoders[i] = new SimpleMessageDecoder(TypeLoaderImpl.DEFAULT_INSTANCE, CodecFactory.COMPILED, types[i]);
            classes[i] = encoders[i].getClassInfo().getTargetClass();
        }

        typeMap = new RecordTypeMap<Class>(classes);
    }

    @Override
    public int                  compareTo(InstrumentMessage o) {
        if (nanos == o.getNanoTime())
            return  Util.fastCompare(symbol, o.getSymbol());

        return nanos > o.getNanoTime() ? 1 : -1;
    }
}
