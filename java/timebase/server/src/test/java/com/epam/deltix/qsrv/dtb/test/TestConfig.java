package com.epam.deltix.qsrv.dtb.test;

import com.epam.deltix.qsrv.dtb.fs.local.*;
import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.util.io.*;

/**
 *
 */
public class TestConfig {
    public int              batchSize = 1;
    public int              numMessages = 10000;
    public int              numEntities = 10;
    public int              numTypes = 4;
    public int              maxFileSize = 5 << 10;
    public int              maxFolderSize = 10;
    public String           compression = "LZ4";
    public long             baseTime = 1356998400000000000L;
    public AbstractPath     path =
        FSFactory.getLocalFS().createPath(Home.getPath("temp/testdtb"));
}
