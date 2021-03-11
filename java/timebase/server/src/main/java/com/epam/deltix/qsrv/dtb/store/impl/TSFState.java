package com.epam.deltix.qsrv.dtb.store.impl;

/**
 *
 */
public enum TSFState {
    CLEAN_CHECKED_OUT,
    DIRTY_CHECKED_OUT,
    CLEAN_CACHED,
    DIRTY_QUEUED_FOR_WRITE,
}
