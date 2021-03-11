package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.pub.TimeInterval;

class EntityTimeRange {

    public static final long UNDEFINED = Long.MIN_VALUE;

    public long from;
    public long to;

    public boolean writing;
    public boolean  invalidate;

    // last update time
    public long updated;

    public EntityTimeRange() {
        this(Long.MIN_VALUE, Long.MIN_VALUE);
    }

    public EntityTimeRange(long from, long to) {
        this.from = from;
        this.to = to;
    }

    public EntityTimeRange         union(EntityTimeRange r) {
        if (r == null)
            return this;

        if (from == UNDEFINED)
            from = r.from;
        else if (r.from != UNDEFINED)
            from = Math.min(from, r.from);

        if (to == UNDEFINED)
            to = r.to;
        else if (r.to != UNDEFINED)
            to = Math.max(to, r.to);

        return this;
    }

    public EntityTimeRange         union(long[] range) {
        if (range == null)
            return this;

        if (from == UNDEFINED)
            from = range[0];
        else if (range[0] != UNDEFINED)
            from = Math.min(from, range[0]);

        if (to == UNDEFINED)
            to = range[1];
        else if (range[1] != UNDEFINED)
            to = Math.max(to, range[1]);

        return this;
    }

    public void                 set(long[] range) {
        this.from = range != null ? range[0] : Long.MIN_VALUE;
        this.to = range != null ? range[1] : Long.MIN_VALUE;
    }

    public void                 set(TimeInterval range) {
        this.from = range != null ? range.getFromTime() : Long.MIN_VALUE;
        this.to = range != null ? range.getToTime() : Long.MIN_VALUE;
    }

    public long[]               toArray() {
        return isUndefined() ? null : new long[] {from, to};
    }

    public boolean              isUndefined() {
        return (from == Long.MIN_VALUE && to == Long.MIN_VALUE);
    }
}

class StreamRange extends EntityTimeRange {
    public long writers = 0;
}
