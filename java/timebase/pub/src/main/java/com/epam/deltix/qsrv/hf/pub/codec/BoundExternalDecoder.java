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

import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *
 */
public interface BoundExternalDecoder {

    /**
     *  Sets static values into specified message.
     *  @param message  A message object to update.
     */
    public void                 setStaticFields (
        Object                      message
    );

    /**
     *  Decodes an object, starting at current position of the
     *  specified MemoryDataInput.
     *
     *  @param in       A MemoryDataInput to read.
     *  @param message  A message object to decode into.
     */
    public void                 decode (
        MemoryDataInput             in,
        Object                      message
    );
}