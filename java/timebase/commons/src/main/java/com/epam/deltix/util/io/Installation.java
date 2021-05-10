package com.epam.deltix.util.io;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.SecureRandom;
import java.util.Properties;

/**
 *
 */
public class Installation {
    private static final Log LOG = LogFactory.getLog(Installation.class);
    public static final String  INST_PROPS_NAME_LEGACY = "inst.properties";
    public static final String  INST_PROPS_NAME = "install.properties";

    public static final String  SERIAL_PROP_NAME_LEGACY = "serial";
    public static final String  DATE_PROP_NAME_LEGACY = "date";
    public static final String  VERSION_PROP_NAME_LEGACY = "version";
    public static final String  QS_PROP_NAME_LEGACY = "qs.inst";
    public static final String  QO_PROP_NAME_LEGACY = "qo.inst";

    public static final String  SERIAL_PROP_NAME = "SerialProperties.serial";
    public static final String  DATE_PROP_NAME = "SerialProperties.date";
    public static final String  VERSION_PROP_NAME = "SerialProperties.version";
    public static final String  QS_PROP_NAME = "SerialProperties.qsInst";
    public static final String  QO_PROP_NAME = "SerialProperties.qoInst";

    public static final String  COMPONENT_KEY_SUFFIX = ".inst";
    public static final int     COMPONENT_KEY_SUFFIX_LENGTH =
        COMPONENT_KEY_SUFFIX.length ();

    private static boolean isLegacy = false;

    public static String        getSerial () {
        return getSerial(getInstallationProperties(Home.getFile ()));
    }

    public static String        getSerial(Properties p) {
        return getInstProp (p , isLegacy ? SERIAL_PROP_NAME_LEGACY : SERIAL_PROP_NAME);
    }

    public static String        getInstallationDate () {
        return getInstallationDate(getInstallationProperties(Home.getFile ()));
    }

    public static String        getInstallationDate(Properties p) {
        String value = getInstProp(p, isLegacy ? DATE_PROP_NAME_LEGACY : DATE_PROP_NAME);
        return value == null ? "" : value;
    }

    public static String        getVersion () {
        return getVersion(getInstallationProperties(Home.getFile ()));
    }

    public static String        getVersion(Properties p) {
        return getInstProp (p, isLegacy ? VERSION_PROP_NAME_LEGACY : VERSION_PROP_NAME);
    }

    public static String        getQSInst() {
        return getQSInst(getInstallationProperties(Home.getFile ()));
    }

    public static String        getQSInst(Properties p) {
        return getInstProp (p, isLegacy ? QS_PROP_NAME_LEGACY : QS_PROP_NAME);
    }

    public static String        getQOInst() {
        return getQOInst(getInstallationProperties(Home.getFile ()));
    }

    public static String        getQOInst(Properties p) {
        return getInstProp (p, isLegacy ? QO_PROP_NAME_LEGACY : QO_PROP_NAME);
    }
    
    public static File          getInstPropsFile (File instDir) {
        File props = new File (instDir.getParent(), INST_PROPS_NAME);
        if (props.exists()) {
            isLegacy = false;
            return props;
        }
        else {
            isLegacy = true;
            return (new File (instDir, INST_PROPS_NAME_LEGACY));
        }
    }
    
    public static Properties    getInstallationProperties (File instDir) {
        try {
            return (IOUtil.readPropsFromFile (getInstPropsFile (instDir)));
        } catch (FileNotFoundException x) {
            return (null);
        } catch (Throwable x) {
            LOG.error("Failed to read installation properties: %s").with(x);
            return (null);
        }        
    }
    
    private static String       getInstProp (Properties p, String key) {
        try {
            return (p.getProperty (key));
        } catch (Throwable x) {
            LOG.error("Failed to retrieve %s: %s").with(key).with(x);
            return (null);
        }
    }

    public static String          createUID() {
        String s = Long.toHexString(System.currentTimeMillis()) +
                Long.toHexString(new SecureRandom().nextInt(255));
        return s.toUpperCase();
    }
    
    public static void          main (String [] args) {
        System.out.println ("Serial #: " + getSerial ());
    }
}
