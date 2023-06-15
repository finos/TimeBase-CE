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
package com.epam.deltix.izpack.panels.license;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;
import com.epam.deltix.izpack.Utils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class CheckLicenseHelper {

    public final static Logger              LOGGER = Logger.getLogger(CheckLicenseHelper.class.getName());

    private final static String             HTTP_PROXY_HOST = "http.proxyHost";
    private final static String             HTTP_PROXY_PORT = "http.proxyPort";
    private final static String             HTTP_PROXY_USER = "http.proxyUser";
    private final static String             HTTP_PROXY_PASSWORD = "http.proxyPassword";

    private final static String             HTTPS_PROXY_HOST = "https.proxyHost";
    private final static String             HTTPS_PROXY_PORT = "https.proxyPort";
    private final static String             HTTPS_PROXY_USER = "https.proxyUser";
    private final static String             HTTPS_PROXY_PASSWORD = "https.proxyPassword";

    private final InstallData               installData;

    CheckLicenseHelper(final InstallData installData) {
        this.installData = installData;
    }

    void checkLicenseForModules(final List<String> moduleNames, final List<String> errors) {
        modifyExpressPacks();

        try {
            installData.getAllPacks().forEach(pack -> {
                String moduleName = pack.getName();
                String licenseError = checkLicense(moduleName);
                if (licenseError != null && !licenseError.isEmpty()) {
                    moduleNames.add(moduleName);
                    errors.add(licenseError);
                }
            });
        } finally {
            Utils.updateModuleErrors(installData);
        }
    }

    String checkLicense(final String moduleName) {
        if (!Utils.QUANT_SERVER.equalsIgnoreCase(moduleName) && !Utils.QUANT_OFFICE.equalsIgnoreCase(moduleName)) {
            return null;
        }

        setProxyProperties();

        String licenseError;
        try {
//            setLicenseError(moduleName, "Unknown license error");
//
//            OnlineLicenseClient.checkLicense(
//                    installData.getVariable(Utils.SERIAL_VAR),
//                    moduleName,
//                    "",
//                    getModuleVersion(moduleName),
//                    "",
//                    String.valueOf(Runtime.getRuntime().availableProcessors()),
//                    null,
//                    new ArrayList<String>()
//            );

            licenseError = setLicenseError(moduleName, "");
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Error check license for module " + moduleName, e);
            licenseError = setLicenseError(moduleName, e.getMessage());
        }

        return licenseError;
    }

    void setProxyProperties() {
        if (Boolean.valueOf(installData.getVariable(Utils.USE_PROXY_VAR))) {
            String proxyHost = installData.getVariable(Utils.PROXY_HOST_VAR);
            String proxyPort = installData.getVariable(Utils.PROXY_PORT_VAR);

            System.setProperty(HTTP_PROXY_HOST, proxyHost);
            System.setProperty(HTTP_PROXY_PORT, proxyPort);
            System.setProperty(HTTPS_PROXY_HOST, proxyHost);
            System.setProperty(HTTPS_PROXY_PORT, proxyPort);

            if (Boolean.valueOf(installData.getVariable(Utils.PROXY_AUTH_VAR))) {
                String username = installData.getVariable(Utils.PROXY_USERNAME_VAR);
                String password = installData.getVariable(Utils.PROXY_PASSWORD_VAR);

                System.setProperty(HTTP_PROXY_USER, username);
                System.setProperty(HTTP_PROXY_PASSWORD, password);
                System.setProperty(HTTPS_PROXY_USER, username);
                System.setProperty(HTTPS_PROXY_PASSWORD, password);
            } else {
                System.clearProperty(HTTP_PROXY_USER);
                System.clearProperty(HTTP_PROXY_PASSWORD);
                System.clearProperty(HTTPS_PROXY_USER);
                System.clearProperty(HTTPS_PROXY_PASSWORD);
            }
        } else {
            System.clearProperty(HTTP_PROXY_HOST);
            System.clearProperty(HTTP_PROXY_PORT);
            System.clearProperty(HTTPS_PROXY_HOST);
            System.clearProperty(HTTPS_PROXY_PORT);
            System.clearProperty(HTTP_PROXY_USER);
            System.clearProperty(HTTP_PROXY_PASSWORD);
            System.clearProperty(HTTPS_PROXY_USER);
            System.clearProperty(HTTPS_PROXY_PASSWORD);
        }
    }

    private String getModuleVersion(final String moduleName) {
        if (Utils.QUANT_SERVER.equals(moduleName))
            return installData.getVariable(Utils.QS_VERSION_VAR);
        else if (Utils.QUANT_OFFICE.equals(moduleName))
            return installData.getVariable(Utils.QO_VERSION_VAR);
        else
            throw new IllegalArgumentException("Unknown module: " + moduleName);
    }

    private String setLicenseError(final String moduleName, final String error) {
        String licenseError = error;
        if (licenseError != null && !licenseError.isEmpty())
            licenseError = "License error: " + licenseError + "\n";

        if (Utils.QUANT_SERVER.equals(moduleName))
            installData.setVariable(Utils.QS_LICENSE_ERROR_VAR, licenseError);
        else if (Utils.QUANT_OFFICE.equals(moduleName))
            installData.setVariable(Utils.QO_LICENSE_ERROR_VAR, licenseError);
//        else
//            throw new IllegalArgumentException("Unknown module: " + moduleName);

        return error;
    }

    //todo: this method create for express installer
    // should be refactored asap
    private void modifyExpressPacks() {
        if (Utils.isExpress(installData)) {
            List<Pack> selectedPacks = installData.getSelectedPacks();
            installData.getAllPacks().forEach(pack -> {
                if (!selectedPacks.contains(pack)) {
                    selectedPacks.add(pack);
                }
                pack.setPreselected(true);

                try {
                    Field field = pack.getClass().getDeclaredField("required");
                    field.setAccessible(true);
                    field.setBoolean(pack, true);
                } catch (Throwable e) {
                    LOGGER.log(Level.WARNING, "Error set required for express", e);
                }
            });
        }
    }

}