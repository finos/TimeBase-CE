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
package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public class MessageBeanEncoder {
    private final CachedCodecFactory codecFactory;
    private final RawMessage        rawmsg = new RawMessage ();
    private final MemoryDataOutput  out = new MemoryDataOutput (1024);

    public MessageBeanEncoder (CodecFactory codecFactory) {
        this.codecFactory = new CachedCodecFactory (codecFactory);
    }

    public RawMessage       encode (MessageBean bean) {
        out.reset ();

        FixedUnboundEncoder     encoder = codecFactory.createFixedUnboundEncoder (bean.type);

        encoder.beginWrite (out);
        
        while (encoder.nextField ()) {
            String              fieldName = encoder.getField ().getName ();
            Object              value = bean.fields.get (fieldName);

            if (value == null || value == MessageBean.NULL_VALUE)
                encoder.writeNull ();
            else {
                Class <?>       vclass = value.getClass ();

                if (vclass == String.class)
                    encoder.writeString ((String) value);
                else if (vclass == Double.class)
                    encoder.writeDouble ((Double) value);
                else if (vclass == Float.class)
                    encoder.writeDouble ((Float) value);
                else if (vclass == Long.class)
                    encoder.writeLong ((Long) value);
                else if (vclass == Integer.class)
                    encoder.writeInt ((Integer) value);
                else if (vclass == Short.class)
                    encoder.writeInt ((Short) value);
                else if (vclass == Byte.class)
                    encoder.writeInt ((Byte) value);
                else
                    throw new IllegalArgumentException (
                        "Illegal value class " + vclass.getName () +
                        " for field " + fieldName
                    );
            }
        }

        rawmsg.setBytes (out);
        rawmsg.type = bean.type;
        rawmsg.setTimeStampMs(bean.getTimeStampMs());
        rawmsg.setSymbol(bean.getSymbol());

        return (rawmsg);
    }
}