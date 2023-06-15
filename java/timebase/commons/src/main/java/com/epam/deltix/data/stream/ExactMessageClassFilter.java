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
public class ExactMessageClassFilter <T> extends MessageFork <T> {
    private final Class <? extends T>   mClass;
    private final boolean               mPass;
    
    public ExactMessageClassFilter (
        Class <? extends T>         cls,
        MessageChannel<T> acceptedMessageConsumer,
        MessageChannel <T>         rejectedMessageConsumer,
        boolean                     pass
    )
    {
        super (acceptedMessageConsumer, rejectedMessageConsumer);
        
        mClass = cls;
        mPass = pass;
    }
    
    protected boolean               accept (T message) {
        return ((message.getClass () == mClass) == mPass);
    }    
}