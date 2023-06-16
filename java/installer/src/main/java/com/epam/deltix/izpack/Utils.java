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
package com.epam.deltix.izpack;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.prefs.Preferences;

/**
 *
 */
public class Utils {

    public final static boolean             IS_X86;
    public final static boolean             IS_X64;

    static {
        String PROC_ID = System.getenv("PROCESSOR_IDENTIFIER");
        IS_X64 = PROC_ID != null && PROC_ID.contains("64");
        IS_X86 = !IS_X64;
    }

    public final static Preferences         PREFS = Preferences.userNodeForPackage(Utils.class);

    public final static String              INSTALL_PATH_VAR = "INSTALL_PATH";
    public final static String              PLATFORM_VAR = "platform";
    public final static String              COMPILED_PLATFORM_VAR = "compiledPlatform";

    // install.properties
    public final static String              SERIAL_VAR = "SerialProperties.serial";
    public final static String              USE_PROXY_VAR = "SerialProperties.useProxy";
    public final static String              PROXY_HOST_VAR = "SerialProperties.proxyHost";
    public final static String              PROXY_PORT_VAR = "SerialProperties.proxyPort";
    public final static String              PROXY_AUTH_VAR = "SerialProperties.proxyAuth";
    public final static String              PROXY_USERNAME_VAR = "SerialProperties.proxyUsername";
    public final static String              PROXY_PASSWORD_VAR = "SerialProperties.proxyPassword";
    public final static String              INSTALLATION_NAME_VAR = "InstallationNameProperties.instName";
    public final static String              INST_FOLDER_VAR = "InstDataProperties.instFolder";
    public final static String              INSTALL_VERSION_VAR = "SerialProperties.version";
    public final static String              INSTALL_QO_INST_VAR = "SerialProperties.qoInst";
    public final static String              INSTALL_QS_INST_VAR = "SerialProperties.qsInst";

    // legacy inst.properties
    public final static String              NUM_PROCESSORS_VAR = "numberProcessors";
    public final static String              UID_VAR = "uid";
    public final static String              INSTALL_DATE_VAR = "date";
    public final static String              SERIAL_INST_VAR = "serial";
    public final static String              VERSION_VAR = "version";
    public final static String              QO_INST_VAR = "qo.inst";
    public final static String              QS_INST_VAR = "qs.inst";

    // packs
    public final static String              QUANT_SERVER = "QuantServer";
    public final static String              QUANT_OFFICE = "QuantOffice";
    public final static String              TIMEBASE = "Timebase";
    public final static String              WEB_GATEWAY = "WebGateway";

    public final static String              WINDOWS_PLATFORM = "windows";
    public final static String              LINUX_PLATFORM = "linux";
    public final static String              MACOS_PLATFORM = "macOS";

    public final static String              QS_VERSION_VAR = "qs.version";
    public final static String              QO_VERSION_VAR = "qo.version";
    public final static String              QS_ERROR_VAR = "qs.error";
    public final static String              QO_ERROR_VAR = "qo.error";
    public final static String              QS_LICENSE_ERROR_VAR = "qs.license.error";
    public final static String              QO_LICENSE_ERROR_VAR = "qo.license.error";
    public final static String              QS_REQUIREMENTS_ERROR_VAR = "qs.requirements.error";
    public final static String              QO_REQUIREMENTS_ERROR_VAR = "qo.requirements.error";

    public final static String              MIN_DOTNET_VERSION = "qo.min.dotnet.version";

    public final static String              QO_EXPRESS = "qoExpress";

    //strings
    public final static String              WRONG_PLATFORM_STR = "wrong.platform";
    public final static String              WRONG_PLATFORM_ERROR_STR = "wrong.platform.error";
    public final static String              WELCOME_MESSAGE_STR = "console.welcome.message";
    public final static String              PROGRAM_FILES_X86_STR = "programFiles.x86.warning";
    public final static String              LICENSE_ERROR_STR = "license.error.title";
    public final static String              REQUIREMENTS_ERROR_STR = "requirements.error.title";
    public final static String              ERROR_TEXT_1_STR = "error.text1";
    public final static String              PROCEED_STR = "error.proceed";
    public final static String              FINISH_LAUNCH_ARCHITECT_STR= "finish.launch.architect";
    public final static String              FINISH_LAUNCH_QO_STR= "finish.launch.qo";

    public final static String              QSADMIN_WINDOWS = "QuantServer/bin/qsadmin.cmd";
    public final static String              QSADMIN_LINUX = "QuantServer/bin/qsadmin.sh";

    public final static String              QO_LAUNCHER_WINDOWS = "QuantOffice/Bin/QuantOfficeShell.exe";

    public static String                    getSystemDrive() {
        String sysdrive = System.getenv("SYSTEMDRIVE");

        if (sysdrive == null)
            sysdrive = "C:";

        return (sysdrive);
    }

    public static  String                   getProgramFiles() {
        String pf = System.getenv ("ProgramFiles");

        if (pf == null)
            pf = getSystemDrive () + "\\Program Files";

        return (pf);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getPrivateValue(final Class<?> cls, final Object object, final String fieldName)
            throws NoSuchFieldException, IllegalAccessException
    {
        Field field = cls.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object o = field.get(object);
        if (o != null)
            return (T) o;

        return null;
    }

    public static void updateModuleErrors(InstallData installData) {
        //set error messages
        String qsError = installData.getVariable(Utils.QS_REQUIREMENTS_ERROR_VAR) + installData.getVariable(Utils.QS_LICENSE_ERROR_VAR);
        if (!qsError.isEmpty()) {
            qsError = "\nWARNING\n" + qsError;
        }
        installData.setVariable(Utils.QS_ERROR_VAR, qsError);

        String qoError = installData.getVariable(Utils.QO_REQUIREMENTS_ERROR_VAR) + installData.getVariable(Utils.QO_LICENSE_ERROR_VAR);
        if (!qoError.isEmpty()) {
            qoError = "\nWARNING\n" + qoError;
        }
        installData.setVariable(Utils.QO_ERROR_VAR, qoError);
    }

    public static boolean isExpress(InstallData installData) {
        String qoExpress = installData.getVariable(Utils.QO_EXPRESS);
        if ("true".equals(qoExpress)) {
            return true;
        }

        return false;
    }

    public static void mkDirIfNeeded(File f) throws FileNotFoundException {
        if (!f.isDirectory() && !f.mkdirs()) {
            throw new FileNotFoundException("Cannot create " + f.getPath());
        }
    }

    public static String getJVMPath() throws IOException {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        String[] items = bean.getLibraryPath().split(File.pathSeparator);

        for (String path : items) {
            Path jvm = Paths.get(path,  "server", "jvm.dll");
            if (jvm.toFile().exists())
                return jvm.toFile().getCanonicalPath();
        }

        return null;
    }

    public static boolean isPackSelected(InstallData installData, String packName) {
        List<Pack> packs = installData.getSelectedPacks();
        for (Pack pack : packs) {
            if (packName.equals(pack.getName())) {
                return true;
            }
        }

        return false;
    }

}