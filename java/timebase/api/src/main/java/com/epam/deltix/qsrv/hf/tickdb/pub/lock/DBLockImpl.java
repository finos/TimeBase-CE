package com.epam.deltix.qsrv.hf.tickdb.pub.lock;

public abstract class DBLockImpl implements DBLock {
    private final String    guid; // client id
    private LockType        type;

    public DBLockImpl(LockType type, String guid) {
        this.type = type;
        this.guid = guid;
    }

    public String       getGuid() {
        return guid;
    }

    @Override
    public LockType     getType() {
        return type;
    }

    @Override
    public int          hashCode() {
        return guid != null ? guid.hashCode() : 0;
    }

    @Override
    public boolean      equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof DBLockImpl)
            return guid.equals(((DBLockImpl) o).guid);

        return false;
    }
}
