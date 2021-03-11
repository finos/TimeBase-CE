package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import org.apache.hadoop.io.LongWritable;

import java.io.IOException;

/**
 *
 */
public class WritableMessageChannel<T extends WritableMessage> implements MessageChannel<InstrumentMessage> {
    private final MessageMapper<LongWritable, T>.Context context;
    private final LongWritable time = new LongWritable(0);
    private final T writable;

    public WritableMessageChannel(final T writable, final MessageMapper<LongWritable, T>.Context context) {
        this.writable = writable;
        this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void send(InstrumentMessage msg) {
        try {
            time.set(msg.getNanoTime());
            writable.set(msg, context);
            context.write(time, writable);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
    }
}
