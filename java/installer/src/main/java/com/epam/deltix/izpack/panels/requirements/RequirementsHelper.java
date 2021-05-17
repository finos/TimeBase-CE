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
package com.epam.deltix.izpack.panels.requirements;

import com.izforge.izpack.api.data.InstallData;
import com.epam.deltix.izpack.RegistryUtils;
import com.epam.deltix.izpack.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequirementsHelper {

    final static Logger LOGGER = Logger.getLogger(RequirementsHelper.class.getName());

    private final static String UNKNOWN_VERSION = "UNKNOWN";
    private final static String SKIP_REQUIREMENTS_CHECK = "requirements.skip";

    private final InstallData installData;

    RequirementsHelper(final InstallData installData) {
        this.installData = installData;
    }

    void checkRequirements(final List<String> moduleNames, final List<String> errors) {
        try {
            boolean skipRequirements = Boolean.getBoolean(SKIP_REQUIREMENTS_CHECK);
            if (skipRequirements) {
                return;
            }

            installData.getAllPacks().forEach(pack -> {
                String moduleName = pack.getName();
                String licenseError = checkRequirements(moduleName);
                if (licenseError != null && !licenseError.isEmpty()) {
                    moduleNames.add(moduleName);
                    errors.add(licenseError);
                }
            });
        } finally {
            Utils.updateModuleErrors(installData);
        }
    }

    private String checkRequirements(String moduleName) {
        if (Utils.QUANT_OFFICE.equals(moduleName)) {
            String minDotnetVersion = installData.getVariable(Utils.MIN_DOTNET_VERSION);
            if (minDotnetVersion == null || minDotnetVersion.isEmpty()) {
                return null;
            }

            String error = checkQORequirement(installData.getVariable(Utils.PLATFORM_VAR).toLowerCase(), minDotnetVersion);
            if (!error.isEmpty()) {
                installData.setVariable(Utils.QO_REQUIREMENTS_ERROR_VAR, error);
            }

            return error;
        }

        return null;
    }

    private String checkQORequirement(String platform, String requiredVersion) {
        if (Utils.WINDOWS_PLATFORM.equalsIgnoreCase(platform)) {
            String actualVersion = detectDotNetVersion();
            if (!checkMinVersion(requiredVersion, actualVersion)) {
                return "Required version of .NET Framework is " + requiredVersion + ". " +
                        "Actual version: " + actualVersion + ". \n";
            }
        } else if (Utils.LINUX_PLATFORM.equalsIgnoreCase(platform) || Utils.MACOS_PLATFORM.equalsIgnoreCase(platform)) {
            String actualVersion = detectMonoVersion();
            if (!checkMinVersion(requiredVersion, actualVersion)) {
                return "Required version of Mono is " + requiredVersion + ". " +
                        "Actual version: " + actualVersion + ". \n";
            }
        }

        return "";
    }

    private String detectDotNetVersion() {
        try {
            String release = RegistryUtils.valueForKey(RegistryUtils.HKEY_LOCAL_MACHINE,
                    "SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v4\\Full", "Release");

            if (release != null) {
                int version = Integer.decode(release);
                if (version >= 528040)
                    return "4.8.0";
                if (version >= 461808)
                    return "4.7.2";
                if (version >= 461308)
                    return "4.7.1";
                if (version >= 460798)
                    return "4.7";
                if (version >= 394802)
                    return "4.6.2";
                if (version >= 394254)
                    return "4.6.1";
                if (version >= 393295)
                    return "4.6";
                if (version >= 379893)
                    return "4.5.2";
                if (version >= 378675)
                    return "4.5.1";
                if (version >= 378389)
                    return "4.5";
            }

            release = RegistryUtils.valueForKey(RegistryUtils.HKEY_LOCAL_MACHINE,
                    "SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v3.5", "Version");
            if (release != null)
                return "3.5";

            release = RegistryUtils.valueForKey(RegistryUtils.HKEY_LOCAL_MACHINE,
                    "SOFTWARE\\Microsoft\\NET Framework Setup\\NDP\\v3.0", "Version");
            if (release != null)
                return "3.0";
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error to detect version of .Net Framework", t);
        }

        return UNKNOWN_VERSION;
    }

    private String detectMonoVersion() {
        String version = UNKNOWN_VERSION;

        try {
            Process p = Runtime.getRuntime().exec("mono --version");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            String versionWord = "compiler version ";
            while ((line = input.readLine()) != null) {
                int indexStart = line.indexOf(versionWord);
                if (indexStart > 0) {
                    indexStart = indexStart + versionWord.length();
                    int indexEnd = line.indexOf(" ", indexStart);
                    if (indexEnd > 0) {
                        version = line.substring(indexStart, indexEnd);
                        break;
                    }
                }
            }
            p.waitFor();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error to detect version of Mono", t);
        }

        return version;
    }

    private boolean checkMinVersion(String minVer, String actualVer) {
        if (UNKNOWN_VERSION.equalsIgnoreCase(actualVer))
            return false;

        String[] min = minVer.split("\\.");
        String[] actual = actualVer.split("\\.");
        int count = Math.max(min.length, actual.length);

        for (int i = 0; i < count; ++i) {
            String minCur = i < min.length ? min[i] : "0";
            String actualCur = i < actual.length ? actual[i] : "0";

            int diff = Integer.valueOf(minCur).compareTo(Integer.valueOf(actualCur));
            if (diff < 0)
                return true;
            if (diff > 0)
                return false;
        }

        return true;
    }
}
