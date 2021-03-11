package com.epam.deltix.util.os;

import com.epam.deltix.util.lang.Util;

import java.io.File;

public class ServiceControlFactory {
    private static final ServiceControl INSTANCE;

    static {
        if (Util.IS_WINDOWS_OS) {
            boolean isNative = !Boolean.getBoolean("service.control.native.disable");
            if (isNative)
                INSTANCE = WindowsNativeServiceControl.INSTANCE;
            else
                INSTANCE = WindowsServiceControl.INSTANCE;
        } else {
            if (new File("/usr/sbin/update-rc.d").exists() || new File("/usr/sbin/chkconfig").exists()) {
                INSTANCE = SystemdServiceControl.INSTANCE;
//                INSTANCE = DebianFamilyServiceControl.INSTANCE;
//            } else if (new File("/usr/sbin/chkconfig").exists()) {
//                INSTANCE = RedHatFamilyServiceControl.INSTANCE;
            } else {
                throw new IllegalStateException("Unsupported OS.");
            }
        }
    }

    public static ServiceControl getInstance() {
        return INSTANCE;
    }
}
