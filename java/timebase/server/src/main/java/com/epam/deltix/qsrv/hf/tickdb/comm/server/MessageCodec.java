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
package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.RecordDecoder;
import com.epam.deltix.qsrv.hf.tickdb.pub.CommonOptions;
import com.epam.deltix.timebase.messages.InstrumentMessage;

public abstract class MessageCodec {

    public static RecordDecoder<InstrumentMessage> createDecoder(RecordClassDescriptor[] types, CommonOptions options) {
        if (options.raw)
            return new SimpleRawDecoder(types);

        boolean compiled = options.channelQOS == ChannelQualityOfService.MAX_THROUGHPUT;

        return new PolyBoundDecoder(options.getTypeLoader(), CodecFactory.get(compiled), types);
    }
}