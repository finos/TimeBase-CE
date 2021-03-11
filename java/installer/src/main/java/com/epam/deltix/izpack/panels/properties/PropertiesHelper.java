package com.epam.deltix.izpack.panels.properties;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.data.Pack;
import com.epam.deltix.izpack.Utils;

import java.io.*;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class PropertiesHelper {

    final static Logger LOGGER = Logger.getLogger(PropertiesHelper.class.getName());

    private final static String         INSTALL_PROPERTIES = "install.properties";
    private final static String         INST_PROPERTIES = Utils.QUANT_SERVER + "/inst.properties";

    private final static String[]       INSTALL_PROP_NAMES = {
            Utils.SERIAL_VAR,
            Utils.USE_PROXY_VAR,
            Utils.PROXY_HOST_VAR,
            Utils.PROXY_PORT_VAR,
            Utils.PROXY_AUTH_VAR,
            Utils.PROXY_USERNAME_VAR,
            Utils.PROXY_PASSWORD_VAR,
            Utils.INSTALLATION_NAME_VAR,
            Utils.INST_FOLDER_VAR,
            Utils.INSTALL_VERSION_VAR,
            Utils.INSTALL_QO_INST_VAR,
            Utils.INSTALL_QS_INST_VAR
    };

    private final InstallData installData;

    PropertiesHelper(final InstallData installData) {
        this.installData = installData;
    }

    String checkPlatform() {
        String compiledPlatform = installData.getVariable(Utils.COMPILED_PLATFORM_VAR);
        String platform = installData.getVariable(Utils.PLATFORM_VAR);

        // allow running linux installer for mac OS
        if (Utils.MACOS_PLATFORM.equalsIgnoreCase(platform) && Utils.LINUX_PLATFORM.equalsIgnoreCase(compiledPlatform)) {
            return null;
        }

        if (!compiledPlatform.equalsIgnoreCase(platform))
            return installData.getVariables().replace(installData.getMessages().get(Utils.WRONG_PLATFORM_ERROR_STR));

        return null;
    }

    void loadProperties() {
        for (final String name : INSTALL_PROP_NAMES) {
            String value = Utils.PREFS.get(name, null);
            if (value != null) {
                LOGGER.fine("Read property (Name: " + name + "; Value: " + value + ")");
                installData.setVariable(name, value);
            }
        }
    }

    void storeProperties() {
        // save additional properties
        saveAdditionalProperties();

        // trim installation name
        String installationName = installData.getVariable(Utils.INSTALLATION_NAME_VAR);
        if (installationName != null)
            installData.setVariable(Utils.INSTALLATION_NAME_VAR, installationName.trim());

        String installPath = installData.getVariable(Utils.INSTALL_PATH_VAR);
        if (installPath != null) {
            File quantServer = new File(installPath, Utils.QUANT_SERVER);
            if (!quantServer.exists())
                quantServer.mkdirs();

            writeProperties(
                installData, new File(installPath, INSTALL_PROPERTIES), INSTALL_PROP_NAMES,
                isPackSelected(Utils.QUANT_SERVER) || isPackSelected(Utils.QUANT_OFFICE),
                true
            );
            if (isPackSelected(Utils.QUANT_SERVER)) {
                writeInstProperties(installData, new File(installPath, INST_PROPERTIES), false);
            }
        }
    }

    private void saveAdditionalProperties() {
        installData.setVariable(Utils.NUM_PROCESSORS_VAR, String.valueOf(Runtime.getRuntime().availableProcessors()));
        installData.setVariable(Utils.INSTALL_DATE_VAR, getCurrentDate());
        installData.setVariable(Utils.UID_VAR, createUID());
        installData.setVariable(Utils.INSTALL_VERSION_VAR, installData.getVariable(Utils.QS_VERSION_VAR));

        // set qs.inst and qo.inst properties
        if (isPackSelected(Utils.QUANT_OFFICE)) {
            installData.setVariable(Utils.QO_INST_VAR, "yes");
            installData.setVariable(Utils.INSTALL_QO_INST_VAR, "yes");
        } else {
            installData.setVariable(Utils.QO_INST_VAR, "no");
            installData.setVariable(Utils.INSTALL_QO_INST_VAR, "no");
        }

        if (isPackSelected(Utils.QUANT_SERVER)) {
            installData.setVariable(Utils.QS_INST_VAR, "yes");
            installData.setVariable(Utils.INSTALL_QS_INST_VAR, "yes");
        } else {
            installData.setVariable(Utils.QS_INST_VAR, "no");
            installData.setVariable(Utils.INSTALL_QS_INST_VAR, "no");
        }
    }

    private boolean isPackSelected(String packName) {
        List<Pack> packs = installData.getSelectedPacks();
        for (Pack pack : packs) {
            if (packName.equals(pack.getName())) {
                return true;
            }
        }

        return false;
    }

    private void writeProperties(final InstallData installData, final File file, final String[] propNames, boolean store, boolean savePrefs) {
        LOGGER.fine("Writing properties to file: " + file.getAbsolutePath());

        try {
            Properties properties = new Properties();
            for (final String property : propNames) {
                addProperty(properties, property, installData.getVariable(property), savePrefs);
            }

            if (store) {
                try (OutputStream output = new FileOutputStream(file)) {
                    properties.store(output, null);
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error writing properties", t);
            t.printStackTrace();
        }
    }

    private void writeInstProperties(final InstallData installData, final File file, boolean savePrefs) {
        LOGGER.fine("Writing properties to file: " + file.getAbsolutePath());

        Properties properties = new Properties();
        try (OutputStream output = new FileOutputStream(file)) {
            addProperty(properties, Utils.SERIAL_INST_VAR, installData.getVariable(Utils.SERIAL_INST_VAR), savePrefs);
            addProperty(properties, Utils.NUM_PROCESSORS_VAR, installData.getVariable(Utils.NUM_PROCESSORS_VAR), savePrefs);
            addProperty(properties, Utils.QO_INST_VAR, installData.getVariable(Utils.QO_INST_VAR), savePrefs);
            addProperty(properties, Utils.QS_INST_VAR, installData.getVariable(Utils.QS_INST_VAR), savePrefs);
            addProperty(properties, Utils.VERSION_VAR, installData.getVariable(Utils.QS_VERSION_VAR), savePrefs);
            addProperty(properties, Utils.INSTALL_DATE_VAR, installData.getVariable(Utils.INSTALL_DATE_VAR), savePrefs);
            addProperty(properties, Utils.UID_VAR, installData.getVariable(Utils.UID_VAR), savePrefs);

            properties.store(output, null);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error writing properties", t);
            t.printStackTrace();
        }
    }

    private void addProperty(Properties properties, String property, String value, boolean savePrefs) {
        if (value != null) {
            LOGGER.fine("Name: '" + property + "; Value: " + value);
            properties.setProperty(property, value);

            if (savePrefs) {
                Utils.PREFS.put(property, value);
            }
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    private static String createUID() {
        String s = Long.toHexString(System.currentTimeMillis()) +
                Long.toHexString(new SecureRandom().nextInt(255));
        return s.toUpperCase();
    }
}
