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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 * Created by Alex Karpovich on 29/07/2020.
 */
public class SimpleBoundEncoder implements MessageEncoder<InstrumentMessage> {

    private final TypeLoader loader;

    private final RecordTypeMap<Class> typeMap;
    private final FixedBoundEncoder[] encoders;
    private final boolean[] validated; // indicated if encoder is validated
    private int code;

    public SimpleBoundEncoder(CodecFactory factory, TypeLoader loader, RecordClassDescriptor[] types) {
        this.loader = loader;

        final int numTypes = types.length;

        encoders = new FixedBoundEncoder[numTypes];
        validated = new boolean[numTypes];

        final Class<?>[] classes = new Class<?>[numTypes];

        for (int ii = 0; ii < numTypes; ii++) {
            if (types[ii] == null)
                continue;

            FixedBoundEncoder encoder = factory.createFixedBoundEncoder(loader, types[ii]);

            CodecUtils.validateBoundClass(encoder.getClassInfo());

            encoders[ii] = encoder;
            classes[ii] = encoder.getClassInfo().getTargetClass();
        }

        typeMap = new RecordTypeMap<>(classes);
    }

    @Override
    public boolean encode(InstrumentMessage message, MemoryDataOutput out) {
        code = typeMap.getCode(message.getClass());

        if (validated.length > 1)
            out.writeUnsignedByte(code);

        out.writeString(message.getSymbol());

        FixedBoundEncoder encoder = encoders[code];

        if (!validated[code]) {
            CodecUtils.validateBoundClass(encoder.getClassInfo());
            validated[code] = true;
        }

        encoder.encode(message, out);
        return true;
    }

    @Override
    public int getContentOffset() {
        return 0;
    }

    @Override
    public int getTypeIndex() {
        return 0;
    }
}
