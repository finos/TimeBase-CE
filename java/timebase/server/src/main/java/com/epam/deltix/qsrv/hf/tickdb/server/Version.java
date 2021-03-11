package com.epam.deltix.qsrv.hf.tickdb.server;

/**
 * Timebase Server version string
 */
public class Version {

    // valid only when package exists only in single jar
    private static final String version = Version.class.getPackage().getImplementationVersion();

    public static String    getVersion() {
        return version;
    }
}
