package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *
 */
public interface DBStateNotifier {

    void            addStateListener(DBStateListener listener);
    void            removeStateListener(DBStateListener listener);

    void            fireStateChanged(final String key);
    void            fireAdded(final String key);
    void            fireDeleted(final String key);
    void            fireRenamed(final String fromKey, final String toKey);
}
