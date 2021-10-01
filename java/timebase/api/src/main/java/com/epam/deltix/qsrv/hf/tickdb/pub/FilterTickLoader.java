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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import com.epam.deltix.timebase.messages.MessageInfo;

import java.io.IOException;

/**
 *  Base class for delegating implementations of TickLoader.
 */
public class FilterTickLoader<T extends MessageInfo> implements TickLoader<T> {
    private final TickLoader<T>        delegate;

    public FilterTickLoader (TickLoader<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public String toString () {
        return getClass ().getName () + " (" + delegate.toString () + ")";
    }

    public void close () {
        delegate.close ();
    }       

    public void send (T msg) {
        delegate.send (msg);
    }

    public WritableTickStream getTargetStream () {
        return delegate.getTargetStream ();
    }
    
    @Override
    public void removeEventListener (LoadingErrorListener listener) {
        delegate.removeEventListener (listener);
    }

    @Override
    public void addEventListener (LoadingErrorListener listener) {
        delegate.addEventListener (listener);
    }

    @Override
    public void addSubscriptionListener(SubscriptionChangeListener listener) {        
        delegate.addSubscriptionListener(listener);
    }

    @Override
    public void removeSubscriptionListener(SubscriptionChangeListener listener) {
        delegate.removeSubscriptionListener(listener);
    }

    @Override
    public void removeUnique(T msg) {
        delegate.removeUnique(msg);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }
}