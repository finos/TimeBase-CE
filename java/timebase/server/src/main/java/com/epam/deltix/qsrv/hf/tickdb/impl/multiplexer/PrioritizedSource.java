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
