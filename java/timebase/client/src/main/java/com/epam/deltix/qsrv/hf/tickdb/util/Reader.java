package com.epam.deltix.qsrv.hf.tickdb.util;

import java.io.Closeable;

/**
 *
 */
public interface Reader extends Closeable {

    boolean             next();

    Object getMessage();
}