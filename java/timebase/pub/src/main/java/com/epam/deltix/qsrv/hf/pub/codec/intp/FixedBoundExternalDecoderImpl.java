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

import com.epam.deltix.qsrv.hf.codec.cg.CharSequencePool;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.lang.reflect.InvocationTargetException;

/**
 *  Interpreting BoundExternalDecoder
 */
public class FixedBoundExternalDecoderImpl implements FixedExternalDecoder {
    protected final RecordLayout        layout;
    private final FieldDecoder []       nonStaticFields;
    private final DecodingContext       ctxt;

    public FixedBoundExternalDecoderImpl (RecordLayout layout) {
        if (!layout.isBound ())
            throw new IllegalArgumentException (layout + " is not bound");
        
        this.layout = layout;
        
        nonStaticFields = FieldCodecFactory.createDecoders (layout);
        ctxt = new DecodingContext (layout);
    }

    public RecordClassInfo      getClassInfo () {
        return (layout);
    }

    public Class<?>             getTargetClass () {
        return (layout.getTargetClass ());
    }

    public void                 setStaticFields (Object message) {
        layout.setStaticFields (message);
    }

    public void                 decode (DecodingContext external, Object msgObject) {

        if (layout.getTargetClass () != msgObject.getClass ())
            throw new IllegalArgumentException (
                    "Object class " + msgObject.getClass () +
                            " does not match " + layout.getTargetClass () +
                            ", to which this decoder is bound."
            );

        if (ctxt != external)
            ctxt.setInput(external);

        boolean truncated = false;
        for (FieldDecoder f : nonStaticFields) {
            try {
                if (truncated || (truncated = ctxt.in.getAvail() <= 0)) {
                    if (f.isBound()) {
                        assert f.isNullable : f.getNotNullableMsg();
                        if (!f.isNullable)
                            throw new IllegalArgumentException(f.getNotNullableMsg());
                        f.setNull(msgObject);
                    }
                } else {
                    // skip dummy field
                    if (!f.isBound())
                        f.skip(ctxt);
                    else
                        f.copy(ctxt, msgObject);
                }
            } catch (InvocationTargetException | IllegalAccessException ex) {
                throw new RuntimeException (f.toString (), ex);
            }
        }
    }
    
    public void                 decode (MemoryDataInput in, Object msgObject) {
        ctxt.setInput(in);
        decode(ctxt, msgObject);
    }
}