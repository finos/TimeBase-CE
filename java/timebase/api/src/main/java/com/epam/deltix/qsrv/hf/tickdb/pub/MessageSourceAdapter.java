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

import com.epam.deltix.streaming.MessageSource;

public class MessageSourceAdapter<T> implements MessageSource <T>  {
    private final MessageSource<T> delegate;

    public MessageSourceAdapter(MessageSource<T> delegate) {
        this.delegate = delegate;
    }

    protected MessageSource<T> getDelegate() {
        return delegate;
    }
    
    @Override
    public T getMessage() {
        return delegate.getMessage();
    }

    @Override
    public boolean next() {
        return delegate.next();
    }

    @Override
    public boolean isAtEnd() {
        return delegate.isAtEnd();
    }

    @Override
    public void close() {
        delegate.close();
    }


}
