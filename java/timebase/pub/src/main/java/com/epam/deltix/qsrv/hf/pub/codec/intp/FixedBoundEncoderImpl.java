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
package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.lang.reflect.InvocationTargetException;

/**
 *  Interpreting fixed BoundEncoder
 */
public final class FixedBoundEncoderImpl implements FixedBoundEncoder {
    private final RecordLayout      layout;
    private final FieldEncoder []   fields;
    private final EncodingContext   ctxt;

    public FixedBoundEncoderImpl (RecordLayout layout) {
        if (!layout.isBound ())
            throw new IllegalArgumentException (layout + " is not bound");
        
        this.layout = layout;
        fields = FieldCodecFactory.createEncoders (layout);
        ctxt = new EncodingContext (layout);
    }

    public RecordClassInfo      getClassInfo () {
        return (layout);
    }    
    
    public Class <?>            getTargetClass () {
        return (layout.getTargetClass ());
    }
    
    public void                 encode (Object message, MemoryDataOutput out) {
        ctxt.out = out;

        final short tailIndex = getLastNonNull(message);

        for (int i = 0; i < fields.length && i <= tailIndex; i++) {
            final FieldEncoder f = fields[i];
            try {
                // write NULL for dummy field
                if (!f.isBound()) {
                    if (f.isNullable)
                        f.writeNull(ctxt);
                    else
                        f.throwNotNullableException();
                } else {
                    f.copy(message, ctxt);
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(f.fieldDescription, ex);
            }
        }
    }

    private short getLastNonNull(final Object message) {
        for (int i = fields.length - 1; i >= 0; i--) {
            final FieldEncoder f = fields[i];
            try {
                if (f.isBound()) {
                    if (!f.isNullable || !f.isNullValue(message))
                        return (short) i;
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(f.fieldDescription, ex);
            }
        }
        return -1;
    }
}