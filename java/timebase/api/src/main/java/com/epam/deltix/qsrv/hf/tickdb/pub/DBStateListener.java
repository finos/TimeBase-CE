package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *
 */
public interface DBStateListener {

    void        changed(String key);

    void        added(String key);

    void        deleted(String key);

    default void  renamed(String fromKey, String toKey) {}
}
