package com.epam.deltix.qsrv.hf.tickdb.pub;

public class FSOptions {

    public int     maxFileSize = 1 << 23;

    public int     maxFolderSize = 100;

    public String  compression = "LZ4(5)";

    public String  url = null;

    public FSOptions() {
    }

    public FSOptions(int maxFileSize, int maxFolderSize) {
        this.maxFileSize = maxFileSize;
        this.maxFolderSize = maxFolderSize;
    }

    public FSOptions withMaxFolderSize(int size) {
        this.maxFolderSize = size;
        return this;
    }

    public FSOptions withMaxFileSize(int size) {
        this.maxFileSize = size;
        return this;
    }

    public FSOptions withCompression(String compression) {
        this.compression = compression;
        return this;
    }
}
