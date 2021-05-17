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
package com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer;

import com.epam.deltix.data.stream.MessageSourceMultiplexer;
import com.epam.deltix.data.stream.RealTimeMessageSource;
import com.epam.deltix.streaming.MessageSource;

/**
 * Message source with explicitly set priority.
 *
 * @author Alexei Osipov
 */
public final class PrioritizedSource<T> {
    private final MessageSource<T> src;
    private final int priority;

    private final boolean isRealtimeMessageSource;
    private final RealTimeMessageSource<T> realtimeSrc;

    public PrioritizedSource(MessageSource<T> src, int priority) {
        this.src = src;
        this.priority = priority;
        this.isRealtimeMessageSource = src instanceof RealTimeMessageSource;
        this.realtimeSrc = this.isRealtimeMessageSource ? (RealTimeMessageSource<T>) src : null;
    }

    public RealTimeMessageSource<T> getRealtimeSrc() {
        assert isRealtimeMessageSource;
        return realtimeSrc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != PrioritizedSource.class) return false;

        PrioritizedSource<?> that = (PrioritizedSource<?>) o;
        return src.equals(that.src);
    }

    @Override
    public int hashCode() {
        return src.hashCode();
    }

    public T getMessage() {
        return src.getMessage();
    }

    public boolean isRealtimeMessageSource() {
        return isRealtimeMessageSource;
    }

    public MessageSource<T> getSrc() {
        return src;
    }

    /**
     * Priority for the wrapped message source.
     *
     * Messages from a source with lower priority value are returned by {@link MessageSourceMultiplexer} before
     * messages from a source with higher priority value.
     */
    public int getPriority() {
        return priority;
    }
}
