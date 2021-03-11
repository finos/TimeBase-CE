package com.epam.deltix.qsrv.dtb.fs.local;

import com.epam.deltix.util.io.RandomAccessFileToOutputStreamAdapter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;

/**
 * Created by Alex Karpovich on 17/08/2020.
 */
class RAFAdapter extends RandomAccessFileToOutputStreamAdapter {
    private final boolean isExists;
    private final Path parent;

    public RAFAdapter(RandomAccessFile raf, File file) {
        super(raf);
        this.parent = file.getParentFile().toPath();
        this.isExists = file.exists();
    }

    @Override
    public void close () throws IOException {
        raf.getChannel().force(true);
        raf.close ();

        if (!isExists)
            PathImpl.fsync(parent, true);
    }
}
