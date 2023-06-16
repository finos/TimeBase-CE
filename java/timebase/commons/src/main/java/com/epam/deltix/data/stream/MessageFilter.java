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
 *  Base class for message filters.
 */
public abstract class MessageFilter <T> implements MessageChannel <T> {
    protected final MessageChannel<T> delegate;
    
    public MessageFilter (
        MessageChannel <T>         delegate
    )
    {
        this.delegate = delegate;
    }

    public void                 send (T msg) {
        if (delegate != null)
            delegate.send (msg);
    }


    public void                 close () {
        if (delegate != null)
            delegate.close ();
    }
}