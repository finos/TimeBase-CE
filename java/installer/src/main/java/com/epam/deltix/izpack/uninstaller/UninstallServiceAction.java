/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
