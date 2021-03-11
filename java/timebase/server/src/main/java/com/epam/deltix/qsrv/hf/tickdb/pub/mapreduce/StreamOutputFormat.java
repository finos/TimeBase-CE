package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.*;

import java.io.IOException;

/**
 *
 */
public class StreamOutputFormat extends OutputFormat<LongWritable, InstrumentMessage> {

    public static String    TICKDB_URL = "deltix.mapreduce.output.connection.url";
    public static String    STREAM_KEY = "deltix.mapreduce.output.stream.key";
    public static String    RAW_WRITER = "deltix.mapreduce.writer.raw";

    public static boolean   RAW_WRITER_DEF = false;

    private DXTickDB        connection;
    private DXTickStream    stream;

    @Override
    public synchronized void checkOutputSpecs(JobContext jobContext) throws IOException, InterruptedException {
    }

    @Override
    public synchronized RecordWriter<LongWritable, InstrumentMessage> getRecordWriter(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        Configuration configuration = taskAttemptContext.getConfiguration();
        stream = getStream(configuration);

        LoadingOptions options = new LoadingOptions(configuration.getBoolean(RAW_WRITER, RAW_WRITER_DEF));
        options.writeMode = LoadingOptions.WriteMode.INSERT;
        TickLoader loader = stream.createLoader(options);

        return new StreamWriter(loader);
    }

    @Override
    public synchronized OutputCommitter getOutputCommitter(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        return new StreamOutputCommitter();
    }

    private synchronized DXTickStream getStream(Configuration config) throws IOException {
//        if (connection == null)
//            connection = TickDBFactory.openFromUrl(config.get(TICKDB_URL), false);
//
//        if (stream == null)
//            stream = connection.getStream(config.get(STREAM_KEY));

        return stream;
    }
}