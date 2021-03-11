package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;

import java.io.Closeable;
import java.io.IOException;

public class OutputReader<K extends Writable, V extends Writable> implements Closeable {

    private int                     index = -1;
    private FileStatus[]            fss;
    private SequenceFile.Reader     reader;
    private final FileSystem        fs;
    private Configuration           config;

    public OutputReader(FileSystem fs, Configuration config, String path) throws IOException {
        this.fs = fs;
        this.config = config;

        fss = fs.listStatus(new Path(path), new PathFilter() {
            @Override
            public boolean accept(Path path) {
                return path.getName().startsWith("part-");
            }
        });
    }

    public boolean next(K key, V value) throws IOException {
        SequenceFile.Reader r = getReader();

        if (r != null) {
            if (r.next(key, value))
                return true;

            rollReader();
            return next(key, value);
        }

        return false;
    }

    private void rollReader() throws IOException {
        if (reader != null)
            reader.close();

        try {
            if (++index < fss.length) {
                SequenceFile.Reader.Option option = SequenceFile.Reader.file(fss[index].getPath().makeQualified(fs.getUri(), fs.getWorkingDirectory()));
                reader = new SequenceFile.Reader(config, option);
            } else {
                reader = null;
            }
        } catch (IOException e) {
            reader = null;
        }
    }

    private SequenceFile.Reader getReader() throws IOException {
        while (index < fss.length && reader == null)
            rollReader();

        return reader;
    }

    @Override
    public void close() throws IOException {
        if (reader != null)
            reader.close();
    }
}
