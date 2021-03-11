package com.epam.deltix.qsrv.hf.tickdb.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class DebugFlags {
    public static final Logger LOGGER = Logger.getLogger ("deltix.tickdb.debug");

    public static final boolean     DEBUG_MSG_WRITE = Boolean.getBoolean("deltix.tickdb.debug.write");
    public static final boolean     DEBUG_MSG_READ = Boolean.getBoolean("deltix.tickdb.debug.read");
    public static final boolean     DEBUG_MSG_LOSS = Boolean.getBoolean("deltix.tickdb.debug.loss");
    public static final boolean     DEBUG_MSG_DISCARD = Boolean.getBoolean("deltix.tickdb.debug.discard");

    public static void              discard(String msg) {
        LOGGER.log(Level.INFO, msg);
    }

    public static void              loss(String msg) {
        LOGGER.log(Level.INFO, msg);
    }

    public static void              write(String msg) {
        System.out.println(msg);
    }

    public static void              read(String msg) {
        System.out.println(msg);
    }

}
