package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.util.lang.MathUtil;

import java.util.Comparator;
import java.util.PriorityQueue;

class MessageAccumulator extends PriorityQueue<UnprocessedMessage> {

    MessageAccumulator(int initialCapacity) {
        super(initialCapacity, UMC);
    }

    private volatile int sequence = 0;

    private static final Comparator<UnprocessedMessage> UMC =
        new Comparator <UnprocessedMessage> () {
            public int compare (UnprocessedMessage o1, UnprocessedMessage o2) {
                int result = MathUtil.compare(o1.nanos, o2.nanos);
                if (result == 0)
                    return MathUtil.compare (o1.index, o2.index);
                return result;
            }
        };

    @Override
    public boolean add(UnprocessedMessage msg) {
        msg.index = sequence++;
        return super.add(msg);
    }

    @Override
    public boolean offer(UnprocessedMessage msg) {
        msg.index = sequence++;
        return super.offer(msg);
    }
}

class UnprocessedMessage {
    final long      nanos;
    final byte []   data;
    int             index;
    final long      serial;

    public UnprocessedMessage (long nanos, long serial,
                               byte [] src, int offset, int length) {
        this.serial = serial;
        this.nanos = nanos;
        data = new byte [length];
        System.arraycopy (src, offset, data, 0, length);
    }
}
