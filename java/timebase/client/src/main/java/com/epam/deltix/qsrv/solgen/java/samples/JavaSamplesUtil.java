package com.epam.deltix.qsrv.solgen.java.samples;

import javax.annotation.Nullable;

class JavaSamplesUtil {

    public static boolean isValidClassName(@Nullable String name) {
        return name != null && name.matches("^[A-Z][A-Za-z0-9]*$");
    }

    public static boolean isValidPackageName(@Nullable String name) {
        return name != null && name.matches("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$");
    }

}
