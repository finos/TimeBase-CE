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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.streaming.MessageSource;

import java.util.Arrays;

/**
 * MessageSource implementation backed by an array of messages
 */
public class ArrayMessageSource<T> implements MessageSource<T> {
    private final T[] messages;
    private final int len;
    private T currentMessage;
    private int idx = -1;

    public ArrayMessageSource(T[] messages) {
        this.messages = messages;
        len = messages != null ? messages.length : 0;
    }

    @Override
    public T getMessage() {
        return currentMessage;
    }

    @Override
    public boolean next() {
        if (++idx < len) {
            currentMessage = messages[idx];
            return true;
        } else
            return false;
    }

    @Override
    public boolean isAtEnd() {
        return idx >= len;
    }

    @Override
    public void close() {
        if (messages != null)
            Arrays.fill(messages, null);
    }
}