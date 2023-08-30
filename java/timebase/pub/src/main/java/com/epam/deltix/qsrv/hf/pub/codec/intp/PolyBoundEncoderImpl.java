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
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public class PolyBoundEncoderImpl implements PolyBoundEncoder {
    private final FixedBoundEncoder []                  encoders;
    private final RecordTypeMap<Class>                  classToEncoderMap;
    private final RecordTypeMap<RecordClassDescriptor>  descriptorToEncoderMap;
    
    public PolyBoundEncoderImpl (FixedBoundEncoder [] encoders) {
        this.encoders = encoders; 
        
        int                     num = encoders.length;
        Class <?> []            classes = new Class <?> [num];
        RecordClassDescriptor [] rcds = new RecordClassDescriptor [num];
        
        for (int ii = 0; ii < num; ii++) {
            if (encoders [ii] == null)
                continue;

            classes [ii] = encoders [ii].getClassInfo().getTargetClass();
            rcds [ii] = encoders [ii].getClassInfo ().getDescriptor ();
        }
        
        classToEncoderMap = new RecordTypeMap<Class> (classes);
        descriptorToEncoderMap = new RecordTypeMap<RecordClassDescriptor> (rcds);
    }

    public void             encode (
        RecordClassDescriptor   rcd, 
        Object                  message, 
        MemoryDataOutput        out
    )
    {
        int                     code = descriptorToEncoderMap.getCode (rcd);
        
        out.writeUnsignedByte (code);
        
        encoders [code].encode (message, out);
    }
    
    public void             encode (Object message, MemoryDataOutput out) {
        int                     code = classToEncoderMap.getCode (message.getClass ());
        
        out.writeUnsignedByte (code);
        
        encoders [code].encode (message, out);
    }            
}