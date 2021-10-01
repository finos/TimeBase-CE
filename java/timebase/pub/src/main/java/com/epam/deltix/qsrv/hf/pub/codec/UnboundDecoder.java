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
package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.ValidationError;
import com.epam.deltix.util.memory.MemoryDataInput;


/**
 *  Implements sequential access to unbound records.
 */
public interface UnboundDecoder extends ReadableValue {
    public void                 beginRead (MemoryDataInput in);
    
    public RecordClassInfo      getClassInfo (); 
    
    public boolean              nextField ();
    
    public NonStaticFieldInfo   getField ();
    public boolean              previousField ();
    public boolean              seekField (int index);

    public int                  comparePrimaryKeys (
        MemoryDataInput             in1, 
        MemoryDataInput             in2
    );
    
    public int                  compareAll (
        MemoryDataInput             in1, 
        MemoryDataInput             in2
    );

    public ValidationError validate ();
}