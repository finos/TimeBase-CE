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
package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.FixedUnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.util.memory.MemoryDataOutput;

public class UnboundContainerEncoder implements ContainerEncoder {

    private final RecordTypeMap<RecordClassDescriptor>  map;
    private final FixedUnboundEncoder[]                 encoders;
    private MemoryDataOutput                            out;

    private final MemoryDataOutput                      local = new MemoryDataOutput();
    //private int                                         code = -1;

    public UnboundContainerEncoder(RecordClassDescriptor[] types) {
        map = new RecordTypeMap<> (types);
        encoders = new FixedUnboundEncoder[types.length];

        for (int i = 0; i < types.length; i++)
            encoders[i] = new FixedUnboundEncoderImpl(new RecordLayout(null, types[i]));
    }

    @Override
    public void                         beginWrite(MemoryDataOutput out) {
        this.out = out;
        this.local.reset();
    }

    @Override
    public void                         endWrite() {
        int size = local.getSize();
        MessageSizeCodec.write(size, out);
        out.write(local.getBuffer(), 0, size);
    }

    public FixedUnboundEncoder          getEncoder(RecordClassDescriptor type) {
        int code = map.getCode(type);
        FixedUnboundEncoder encoder = encoders[code];
        encoder.beginWrite(local);

        local.writeUnsignedByte(code);

        return encoder;
    }
}