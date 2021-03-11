package com.epam.deltix.qsrv.dtb.fs.lock.atomicfs;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;

/**
 * @author Alexei Osipov
 */
public class FsLock {
    private final AbstractPath targetPath;
    private final String lockKey;
    private final long timestamp;

    public FsLock(AbstractPath targetPath, String lockKey, long timestamp) {

        this.targetPath = targetPath;
        this.lockKey = lockKey;
        this.timestamp = timestamp;
    }

    public AbstractPath getTargetPath() {
        return targetPath;
    }

    public String getLockKey() {
        return lockKey;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
