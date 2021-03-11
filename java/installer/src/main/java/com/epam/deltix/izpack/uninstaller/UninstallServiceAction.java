package com.epam.deltix.izpack.uninstaller;

import com.izforge.izpack.api.event.AbstractUninstallerListener;
import com.epam.deltix.izpack.Utils;
import com.epam.deltix.util.os.ServiceControl;
import com.epam.deltix.util.os.SystemdServiceControl;
import com.epam.deltix.util.os.WindowsNativeServiceControl;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UninstallServiceAction extends AbstractUninstallerListener {

    private ServiceControl serviceControl;
    private final List<String> installedServices = new ArrayList<>();

    @Override
    public void initialise() {
        File servicesFile = getUninstallFile("services");
        if (servicesFile != null) {
            installedServices.addAll(readServices(servicesFile));
        }

        String platform = null;
        File platformFile = getUninstallFile("platform");
        if (platformFile != null) {
            platform = readPlatform(platformFile);
        }

        boolean isWindows = Utils.WINDOWS_PLATFORM.equalsIgnoreCase(platform);
        serviceControl = isWindows ?
            WindowsNativeServiceControl.INSTANCE : SystemdServiceControl.INSTANCE;

        if (isWindows) {
            File uninstallDir = getUninstallFile();
            if (uninstallDir != null) {
                serviceControl.load(new File(uninstallDir, "resources").getAbsolutePath());
            }
        }
    }

    @Override
    public void beforeDelete(List<File> files) {
        if (serviceControl != null) {
            installedServices.forEach(id -> {
                try {
                    if (serviceControl.exists(id)) {
                        serviceControl.stopAndWait(id, true, 100000);
                        serviceControl.delete(id);
                    }
                } catch (Throwable t) {
                    //ignore
                }
            });
        }
    }

    private File getUninstallFile(String type) {
        File uninstallFile = getUninstallFile();
        if (uninstallFile != null) {
            return new File(uninstallFile, type);
        } else {
            return null;
        }
    }

    private File getUninstallFile() {
        try {
            return new File(UninstallServiceAction.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> readServices(File file) {
        if (file == null) {
            return new ArrayList<>();
        }

        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))) {
            String[] services = (String[]) is.readObject();
            return Arrays.asList(services);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String readPlatform(File file) {
        if (file == null) {
            return Utils.WINDOWS_PLATFORM;
        }

        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(file))) {
            return  (String) is.readObject();
        } catch (Exception e) {
            return Utils.WINDOWS_PLATFORM;
        }
    }

}
