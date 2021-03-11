package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import com.epam.deltix.qsrv.dtb.store.pub.DataReader;
import com.epam.deltix.qsrv.dtb.store.pub.EntityFilter;
import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.impl.Consumer;
import com.epam.deltix.qsrv.hf.tickdb.impl.MessageConsumer;
import com.epam.deltix.qsrv.hf.tickdb.impl.RawConsumer;
import com.epam.deltix.qsrv.hf.tickdb.impl.RegistryCache;
import com.epam.deltix.util.lang.Util;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

public class TSFileReader extends RecordReader<LongWritable, InstrumentMessage> {

    private TSRoot              root;
    private Path[]              files;

    private DataReader          reader;
    private MessageConsumer     consumer;

    private int                 curFile = -1;
    private final LongWritable  key = new LongWritable();
    private long                index = 0;

    public TSFileReader(TSRoot tsRoot, Path[] files, RecordClassSet recordClassSet, boolean isRaw) {
        this.root = tsRoot;
        this.files = files;

        reader = PDSInputFormat.store.createReader(false);
        reader.associate(root);

        if (isRaw)
            consumer = new RawConsumer(new RegistryCache(tsRoot.getSymbolRegistry()), recordClassSet.getContentClasses(), false);
        else
            consumer = new Consumer(new RegistryCache(tsRoot.getSymbolRegistry()), recordClassSet.getContentClasses(),
                TypeLoaderImpl.DEFAULT_INSTANCE,
                CodecFactory.COMPILED, false);
    }

    @Override
    public void initialize(InputSplit split, TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        if (!(split instanceof TSFileInputSplit))
            throw new IllegalArgumentException(split + " is not expected. Required " + TSFileInputSplit.class);
    }

    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
        index++;
        boolean hasNext = reader.readNext(consumer);
        if (!hasNext) {
            if (++curFile >= files.length)
                return false;

            reader.close();
            reader.open(root.associate(files[curFile].toString()), Long.MIN_VALUE, false, EntityFilter.ALL);
            PDSInputFormat.LOGGER.fine("Open file reader: " + files[curFile].toString());

            hasNext = reader.readNext(consumer);
        }
        return hasNext;
    }

    @Override
    public LongWritable getCurrentKey() throws IOException, InterruptedException {
        key.set(index);
        return key;
    }

    @Override
    public InstrumentMessage getCurrentValue() throws IOException, InterruptedException {
        return consumer.getMessage();
    }

    @Override
    public float getProgress() throws IOException, InterruptedException {
        if (curFile < 0)
            return 0;

        if (files.length == 0)
            return 100;

        return curFile / files.length;
    }

    @Override
    public void close() throws IOException {
        Util.close(reader);
    }
}
