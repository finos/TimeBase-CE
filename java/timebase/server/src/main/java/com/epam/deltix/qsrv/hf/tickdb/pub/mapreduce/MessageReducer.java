package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 *
 */
public abstract class MessageReducer<KEYIN, VALUEIN> extends Reducer<KEYIN, VALUEIN, LongWritable, InstrumentMessage> {
    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
    }

    @Override
    protected void reduce(KEYIN key, Iterable<VALUEIN> values, Context context) throws IOException, InterruptedException {
        super.reduce(key, values, context);
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        super.cleanup(context);
    }

    public class OutputMessageChannel implements MessageChannel<InstrumentMessage> {
        private final MessageReducer<KEYIN, VALUEIN>.Context context;
        private final LongWritable time = new LongWritable(0);

        public OutputMessageChannel(final MessageReducer<KEYIN, VALUEIN>.Context context) {
            this.context = context;
        }

        @Override
        public void send(InstrumentMessage msg) {
            try {
                time.set(msg.getNanoTime());
                context.write(time, msg);
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
        }
    }
}
