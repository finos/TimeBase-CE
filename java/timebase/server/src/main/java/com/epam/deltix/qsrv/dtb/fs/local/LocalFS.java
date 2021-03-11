package com.epam.deltix.qsrv.dtb.fs.local;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.util.lang.Util;

import java.io.File;

/**
 *
 */
public class LocalFS implements AbstractFileSystem {

    public LocalFS () {
    }
    
    @Override
    public boolean              isAbsolutePath (String path) {
        return (new File (path).isAbsolute ());
    }
        
    @Override
    public AbstractPath         createPath (String path) {
        return (new PathImpl (path, this));
    }

    @Override
    public AbstractPath         createPath(AbstractPath parent, String child) {
        return new PathImpl((PathImpl) Util.unwrap(parent), child, this);
    }

    @Override
    public long getReopenOnSeekThreshold() {
        return Long.MAX_VALUE;
    }

    @Override
    public String getSeparator() {
        return File.separator;
    }
}
