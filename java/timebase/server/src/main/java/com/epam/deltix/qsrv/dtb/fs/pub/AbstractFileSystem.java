package com.epam.deltix.qsrv.dtb.fs.pub;

/**
 *
 */
public interface AbstractFileSystem {

    boolean              isAbsolutePath (String path);
    
    AbstractPath         createPath (String path);

    AbstractPath         createPath (AbstractPath parent, String child);

    String               getSeparator();

    /**
     * Defines threshold when file should be reopened instead of using "seek" on it's input stream.
     * So if it's needed to skip more than specified number of bytes then}
     * will close existing InputStream and open a new one at requested offset.
     *
     * This may be useful if the File System does not support effective {@link java.io.InputStream#skip(long)}
     * implementation. For example, remote FS like MS Azure DataLake ({@link deltix.qsrv.dtb.fs.azure.AzureFS}).
     *
     * @return size (in bytes) of threshold
     */
    default long getReopenOnSeekThreshold() {
        return 0;
    }

    /**
     * Positive value defines how many data files should be prefetched by singe DataReader when accessing this file system.
     * Zero (default) value means that prefetching should not be used for this file system.
     */
    default int getPrefetchSize() {
        return 0;
    }

    /**
     * FS should return true if {@link AbstractPath#openInput(long, long)} is more preferable than {@link AbstractPath#openInput(long)}.
     * I.e. putting a length limit on read request may provide performance gain for this FS.
     */
    default boolean isReadsWithLimitPreferable() {
        return false;
    }
}
