package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLockImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;

/**
 * User: alex
 * Date: Nov 15, 2010
 */
class ClientLock extends DBLockImpl {
    private final TickStreamClient stream;
    private int usages = 1;

    public ClientLock(TickStreamClient stream, LockType type, String guid) {
        super(type, guid);
        this.stream = stream;
    }

    public void         reuse() {
        synchronized (stream) {
            usages++;
        }
    }

    @Override
    public boolean      isValid() {
        return stream.isValid(this);
    }

    @Override
    public void         release() {
        synchronized (stream) {
            usages--;
            if (usages == 0)
                stream.unlock();
        }
    }
}
