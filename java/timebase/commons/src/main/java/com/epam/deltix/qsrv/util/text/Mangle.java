package com.epam.deltix.qsrv.util.text;

import com.epam.deltix.util.io.IOUtil;

public class Mangle {
    private static final String ENCRYPTION_KEY             = "superKey";
    private static final String ENCRYPTED_VALUE_PREFIX     = "EV";

    public static String concat(String value) {
        String encryptedValue = IOUtil.concat(value, ENCRYPTION_KEY);
        return ENCRYPTED_VALUE_PREFIX + encryptedValue;
    }

    public static String split(String encryptedValue) {
        if(encryptedValue == null)
            return null;

        if(!isEncrypted(encryptedValue))
            return encryptedValue;

        encryptedValue = encryptedValue.substring(ENCRYPTED_VALUE_PREFIX.length());
        return IOUtil.split(encryptedValue, ENCRYPTION_KEY);
    }

    public static boolean isEncrypted(String value){
        return value.startsWith(ENCRYPTED_VALUE_PREFIX);
    }
}
