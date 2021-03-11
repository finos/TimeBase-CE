package com.epam.deltix.qsrv.dtb.fs.pub;

import java.util.StringTokenizer;

/**
 * Created by Alex Karpovich on 15/07/2020.
 */
public final class Constants {
    private Constants() {}  // can't construct

    /** JVM vendor info. */
    public static final String JVM_VENDOR = System.getProperty("java.vm.vendor");
    public static final String JVM_VERSION = System.getProperty("java.vm.version");
    public static final String JVM_NAME = System.getProperty("java.vm.name");
    public static final String JVM_SPEC_VERSION = System.getProperty("java.specification.version");

    /** The value of <code>System.getProperty("java.version")</code>. **/
    public static final String JAVA_VERSION = System.getProperty("java.version");

    /** The value of <code>System.getProperty("os.name")</code>. **/
    public static final String OS_NAME = System.getProperty("os.name");
    /** True iff running on Linux. */
    public static final boolean LINUX = OS_NAME.startsWith("Linux");
    /** True iff running on Windows. */
    public static final boolean WINDOWS = OS_NAME.startsWith("Windows");
    /** True iff running on SunOS. */
    public static final boolean SUN_OS = OS_NAME.startsWith("SunOS");
    /** True iff running on Mac OS X */
    public static final boolean MAC_OS_X = OS_NAME.startsWith("Mac OS X");
    /** True iff running on FreeBSD */
    public static final boolean FREE_BSD = OS_NAME.startsWith("FreeBSD");

    public static final String OS_ARCH = System.getProperty("os.arch");
    public static final String OS_VERSION = System.getProperty("os.version");
    public static final String JAVA_VENDOR = System.getProperty("java.vendor");

    private static final int JVM_MAJOR_VERSION;
    private static final int JVM_MINOR_VERSION;

    /** True iff running on a 64bit JVM */
    public static final boolean JRE_IS_64BIT;

    static {
        final StringTokenizer st = new StringTokenizer(JVM_SPEC_VERSION, ".");
        JVM_MAJOR_VERSION = Integer.parseInt(st.nextToken());
        if (st.hasMoreTokens()) {
            JVM_MINOR_VERSION = Integer.parseInt(st.nextToken());
        } else {
            JVM_MINOR_VERSION = 0;
        }
        boolean is64Bit = false;
        String datamodel = null;
        try {
            datamodel = System.getProperty("sun.arch.data.model");
            if (datamodel != null) {
                is64Bit = datamodel.contains("64");
            }
        } catch (SecurityException ex) {}
        if (datamodel == null) {
            if (OS_ARCH != null && OS_ARCH.contains("64")) {
                is64Bit = true;
            } else {
                is64Bit = false;
            }
        }
        JRE_IS_64BIT = is64Bit;
    }

    public static final boolean JRE_IS_MINIMUM_JAVA8 = JVM_MAJOR_VERSION > 1 || (JVM_MAJOR_VERSION == 1 && JVM_MINOR_VERSION >= 8);
    public static final boolean JRE_IS_MINIMUM_JAVA9 = JVM_MAJOR_VERSION > 1 || (JVM_MAJOR_VERSION == 1 && JVM_MINOR_VERSION >= 9);
    public static final boolean JRE_IS_MINIMUM_JAVA11 = JVM_MAJOR_VERSION > 1 || (JVM_MAJOR_VERSION == 1 && JVM_MINOR_VERSION >= 11);

}
