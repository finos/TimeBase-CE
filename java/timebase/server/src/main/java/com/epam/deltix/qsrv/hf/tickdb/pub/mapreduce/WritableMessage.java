package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import org.apache.hadoop.mapreduce.JobContext;

import java.io.IOException;

/**
 *
 */
public interface WritableMessage<T> {
    public static String MAPPER_OUTPUT_TYPE = "deltix.mapreduce.mapper.output.type";

    public void     set(T message, JobContext context) throws IOException;
    public T        get(JobContext context) throws IOException;
}
