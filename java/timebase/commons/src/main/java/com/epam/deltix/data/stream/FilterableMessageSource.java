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

import com.epam.deltix.streaming.MessageSource;

/**
 *  Decorates a message source with a customizable filter.
 */
public abstract class FilterableMessageSource <T> implements MessageSource <T> {
    protected final MessageSource<T>        source;
    private boolean                         atEnd = false;
    
    protected FilterableMessageSource (MessageSource <T> source) {
        this.source = source;
    }

    public T                            getMessage () {
        return (source.getMessage ());
    }

    public boolean                      isAtEnd () {
        return (atEnd);
    }

    protected abstract boolean          acceptCurrent ();

    public boolean                      next () {
        for (;;) {
            if (!source.next ()) {
                atEnd = true;
                return (false);
            }

            if (acceptCurrent ())
                return (true);
        }
    }

    public void                         close () {
        source.close ();
    }
}