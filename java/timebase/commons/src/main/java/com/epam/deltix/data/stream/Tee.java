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
package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

/**
 *
 */
public class Tee <T> implements MessageChannel<T> {
    private final MessageChannel <T>       mOut1;
    private final MessageChannel <T>       mOut2;
    
    public Tee (MessageChannel <T> out1, MessageChannel <T> out2) {
        mOut1 = out1;
        mOut2 = out2;
    }

    public void         send (T message) {
        if (mOut1 != null)
            mOut1.send (message);
        
        if (mOut2 != null)
            mOut2.send (message);
    }

    public void         close () {
        if (mOut1 != null)
            mOut1.close ();
        
        if (mOut2 != null)
            mOut2.close ();
    }
    
}