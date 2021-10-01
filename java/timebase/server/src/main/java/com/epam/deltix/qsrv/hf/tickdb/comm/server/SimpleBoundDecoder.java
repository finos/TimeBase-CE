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
package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedExternalDecoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.RecordDecoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.Filter;
import com.epam.deltix.util.memory.MemoryDataInput;

public class SimpleBoundDecoder implements RecordDecoder<InstrumentMessage>  {

    private final FixedExternalDecoder      decoder;
    private final RecordClassDescriptor     type;
    private final StringBuilder             symBuilder = new StringBuilder();
    private final InstrumentMessage         instance;


    public SimpleBoundDecoder(RecordClassDescriptor type, TypeLoader loader, CodecFactory factory) {
        this.type = type;
        this.decoder = factory.createFixedExternalDecoder(loader, type);

        this.instance = (InstrumentMessage) type.newInstanceNoX (loader);
        decoder.setStaticFields(instance);
        instance.setSymbol(symBuilder);
    }

    @Override
    public RecordClassDescriptor getCurrentType() {
        return type;
    }

    @Override
    public int getCurrentTypeIndex() {
        return 0;
    }

    @Override
    public InstrumentMessage decode(Filter<? super InstrumentMessage> filter, MemoryDataInput in) {

        in.readStringBuilder(symBuilder);
        decoder.decode(in, instance);

        return filter != null ? (filter.accept(instance) ? instance : null) : instance;
    }
}