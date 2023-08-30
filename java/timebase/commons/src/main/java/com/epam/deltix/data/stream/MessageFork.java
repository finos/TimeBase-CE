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
package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

/**
 *
 */
public abstract class MessageFork <T> implements MessageChannel <T> {
    private final MessageChannel<T> mAcceptedMessageConsumer;
    private final MessageChannel <T>       mRejectedMessageConsumer;
    
    public MessageFork (
        MessageChannel <T>         acceptedMessageConsumer, 
        MessageChannel <T>         rejectedMessageConsumer
    )
    {
        mAcceptedMessageConsumer = acceptedMessageConsumer;
        mRejectedMessageConsumer = rejectedMessageConsumer;
    }

    protected abstract boolean  accept (T message);
    
    public final void           send (T message) {
        MessageChannel <T>         consumer = 
            accept (message) ?
                mAcceptedMessageConsumer :
                mRejectedMessageConsumer;
        
        if (consumer != null)
            consumer.send (message);
    }
    
    public void                 close () {
        if (mAcceptedMessageConsumer != null)
            mAcceptedMessageConsumer.close ();
        
        if (mRejectedMessageConsumer != null)
            mRejectedMessageConsumer.close ();
    }
}