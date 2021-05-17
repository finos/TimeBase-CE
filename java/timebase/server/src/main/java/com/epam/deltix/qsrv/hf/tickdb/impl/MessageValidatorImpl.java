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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.codec.intp.DecodingContext;
import com.epam.deltix.qsrv.hf.pub.codec.intp.FieldCodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.intp.FieldDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.*;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.ValidationError;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataInput;
import java.util.ArrayList;


/**
 *  Interpreting message validator.
 */
public class MessageValidatorImpl implements MessageValidator {
    private final MemoryDataInput           mdi = new MemoryDataInput ();
    private final RecordLayout              layout;
    private final DecodingContext ctxt;
    private final FieldDecoder[]           fields;
    private ArrayList <ValidationError>     errors = new ArrayList <> ();
    
    public MessageValidatorImpl (RecordClassDescriptor rcd) {
        this (new RecordLayout (rcd));
    }
    
    public MessageValidatorImpl (RecordLayout layout) {
        this.layout = layout;
        this.fields = FieldCodecFactory.createDecoders (layout);
        
        ctxt = new DecodingContext (layout);
        ctxt.in = mdi;
    }

    @Override
    public RecordClassInfo      getClassInfo () {
        return (layout);
    }

    @Override
    public int                  validate (RawMessage msg) {
        msg.setUpMemoryDataInput (mdi);
        
        errors.clear ();
        
        final int                   limit = mdi.getLength ();
        final int                   numFields = fields.length;
        
        for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) { 
            final int               offset = mdi.getCurrentOffset ();
            final FieldDecoder      fd = fields [fieldIdx];
            
            if (mdi.getPosition () == limit) {   // trailing null value
                if (!fd.isNullable())
                    errors.add (
                        new IllegalNullValue (
                            offset, 
                            layout.getNonStaticFields () [fieldIdx]                            
                        )
                    );
                
                continue;
            }            
            
            try {
                ValidationError     error = fd.validate (ctxt);
                
                if (error != null)
                    errors.add (error);
                else 
                    ctxt.in.checkAvailable (0);
            } catch (Throwable x) {
                errors.add (
                    new DecodingError (
                        offset, 
                        layout.getNonStaticFields () [fieldIdx],
                        x
                    )
                );
                
                return (errors.size ());
            }                        
        }
        
        if (mdi.getPosition () < limit)
            errors.add (new ExtraDataAtEnd (mdi.getCurrentOffset ()));
        
        return (errors.size ());
    }

    @Override
    public int                  getNumErrors () {
        return (errors.size ());
    }

    @Override
    public ValidationError      getError (int idx) {
        return (errors.get (idx));
    }        
}
