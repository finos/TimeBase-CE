package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;

/**
 *
 */
public class StreamWriter extends RecordWriter<LongWritable, InstrumentMessage> {
    protected TickLoader loader;

    public StreamWriter(TickLoader loader) {
        this.loader = loader;
    }

    @Override
    public void write(LongWritable longWritable, InstrumentMessage msg) throws IOException, InterruptedException {
        if (loader == null)
            throw new NullPointerException("Loader is closed."); // TODO: Consider using another type of exception. Like WriterClosedException or IllegalStateException.

        loader.send(msg);
    }

    @Override
    public void close(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        loader.close();
        loader = null;
    }
}
