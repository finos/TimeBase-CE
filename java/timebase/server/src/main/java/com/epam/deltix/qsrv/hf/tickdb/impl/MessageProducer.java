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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.dtb.store.pub.TSMessageProducer;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataOutput;

public abstract class MessageProducer<T extends InstrumentMessage> implements TSMessageProducer {
    protected T         message;
    protected int       code;

    public abstract int beginWrite(T msg);
}

class RawProducer extends MessageProducer<RawMessage> {

    private final RecordTypeMap<RecordClassDescriptor>      typeMap;

    protected RawProducer(RecordClassDescriptor[] types) {
        typeMap = new RecordTypeMap<RecordClassDescriptor>(types);
    }

    public int beginWrite(RawMessage msg) {
        message = msg;
        return code = typeMap.getCode(message.type);
    }

    @Override
    public void writeBody(MemoryDataOutput out) {
        message.writeTo(out);
    }
}

class Producer extends MessageProducer<InstrumentMessage> {

    private final RecordTypeMap<Class>      typeMap;
    private final FixedBoundEncoder[]       encoders;

    protected Producer(RecordClassDescriptor[] types, TypeLoader loader, CodecFactory factory) {
        this.encoders = new FixedBoundEncoder[types.length];

        final Class <?> []              classes = new Class <?> [types.length];

        for (int i = 0; i < types.length; i++) {
            FixedBoundEncoder encoder = encoders[i] = factory.createFixedBoundEncoder(loader, types[i]);
            CodecUtils.validateBoundClass(encoder.getClassInfo());
            classes [i] = encoder.getClassInfo().getTargetClass ();
        }

        typeMap = new RecordTypeMap<Class> (classes);
    }

    public int beginWrite(InstrumentMessage msg) {
        message = msg;
        return code = typeMap.getCode (message.getClass ());
    }

    @Override
    public void writeBody(MemoryDataOutput out) {
        encoders[code].encode(message, out);
    }
}