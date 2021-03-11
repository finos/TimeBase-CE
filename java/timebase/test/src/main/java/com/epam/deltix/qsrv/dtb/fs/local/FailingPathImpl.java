package com.epam.deltix.qsrv.dtb.fs.local;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.dtb.fs.common.DelegatingAbstractPath;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Alexei Osipov
 */
public class FailingPathImpl extends DelegatingAbstractPath {
    private static final Log LOG = LogFactory.getLog(FailingPathImpl.class);

    private final FailingFileSystem ffs;

    FailingPathImpl(AbstractPath delegate, FailingFileSystem ffs) {
        super(delegate);
        this.ffs = ffs;
    }

    @Override
    protected AbstractPath wrap(AbstractPath path) {
        return ffs.wrap(path);
    }

    @Override
    public AbstractFileSystem getFileSystem() {
        return ffs;
    }

    @Override
    public InputStream openInput(long offset) throws IOException {
        LOG.info("openInput %s").with(this);
        ffs.checkError(getPathString());
        return super.openInput(offset);
    }

    @Override
    public OutputStream openOutput(long size) throws IOException {
        LOG.info("openOutput %s").with(this);
        ffs.checkError(getPathString());
        return super.openOutput(size);
    }
}
