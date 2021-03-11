package com.epam.deltix.qsrv.hf.tickdb.pub.lock;

public interface LockEventListener {

    public void lockAdded(DBLock lock);

    public void lockRemoved(DBLock lock);
}
