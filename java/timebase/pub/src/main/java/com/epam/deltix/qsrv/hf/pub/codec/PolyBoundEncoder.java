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
package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public interface PolyBoundEncoder {
    /**
     *  Encodes an object, starting at current position of the 
     *  specified MemoryDataOutput.
     * 
     *  @param out      A MemoryDataOutput to write to.
     *  @param message  A message object to encode.
     */
    public void                 encode (
        Object                      message,
        MemoryDataOutput            out
    );
    
    /**
     *  Encodes an object, starting at current position of the 
     *  specified MemoryDataOutput. The RecordClassDescriptor argument 
     *  is necessary when there are multiple record class descriptors
     *  bound to the same Java class.
     * 
     *  @param rcd      The descriptor to use.
     *  @param out      A MemoryDataOutput to write to.
     *  @param message  A message object to encode.
     */
    public void                 encode (
        RecordClassDescriptor       rcd,
        Object                      message,
        MemoryDataOutput            out
    );
}