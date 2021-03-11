package com.epam.deltix.qsrv.hf.tickdb.replication;

import java.io.File;

/**
 *
 */
public class FileStorage implements Storage {

    public FileStorage(File folder) {
        this.folder = folder;
    }

    public FileStorage(String folder) {
        this.folder = new File(folder);
    }

    public File folder;
}
