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

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.FixedExternalDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.stream.AbstractMessageReader;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;


import java.io.IOException;

/**
 * Date: Feb 2, 2010
 */
class UniqueMessageReader extends AbstractMessageReader
        implements MessageSource<InstrumentMessage>, TickStreamRelated
{
    private final UniqueMessageContainer container;
    private final TickStreamImpl          stream;
    
    private int                     index = -1;
    private InstrumentMessage       message = null;


    public UniqueMessageReader( UniqueMessageContainer container,
                                SelectionOptions options,
                                TickStreamImpl stream) {
        this.container = container;
        this.stream = stream;
        this.types = stream.getClassDescriptors();

        TypeLoader loader = options.getTypeLoader();

        if (!options.raw) {
            final int       numTypes = this.types.length;

            decoders = new FixedExternalDecoder[numTypes];
            messages = new InstrumentMessage [numTypes];

            for (int i = 0; i < numTypes; i++) {
                FixedExternalDecoder d =
                        decoders[i] = CodecFactory.COMPILED.createFixedExternalDecoder(loader, types[i]);
                InstrumentMessage msg =
                        messages[i] = (InstrumentMessage) types[i].newInstanceNoX(loader);
                d.setStaticFields (msg);
                msg.setSymbol(symbol);
            }
        }
        else {
            rawMsg = new RawMessage();            
            rawMsg.setSymbol(symbol);
        }
    }

    @Override
    public TickStream getStream() {
        return stream;
    }

    @Override
    public InstrumentMessage getMessage() {
        return message;
    }

    public void readMessage() {

        MessageContainer.DataContainer c = container.getMessageData(index);
        buffer.setBytes(c.getBytes(), 0, c.size());

        try {
            message = decode(buffer);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public boolean next() {
        boolean hasData = container.hasData(++index);

        if (hasData)
            readMessage();
            
        return hasData;
    }

    @Override
    public boolean isAtEnd() {
        return !container.hasData(index);
    }

    @Override
    public void close() {
        
    }
}