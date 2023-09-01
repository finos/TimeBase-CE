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
package com.epam.deltix.qsrv.hf.tickdb.schema.encoders;

import com.epam.deltix.qsrv.hf.pub.WritableValue;

public interface MixedWritableValue extends WritableValue {

    public void                 writeBoolean(int value);
    public void                 writeBoolean(long value);
    public void                 writeBoolean(float value);
    public void                 writeBoolean(double value);

    public void                 writeInt (long value);
    public void                 writeInt (double value);

    public void                 writeLong (float value);
    public void                 writeLong (double value);

    public void                 writeFloat (double value);
    
    public void                 writeEnum (CharSequence value);

    public void                 writeDefault();

    public  MixedWritableValue  clone(WritableValue out);
}