package com.epam.deltix.qsrv.hf.pub;

/**
 * 
 */
public class KeyNotFoundException extends Exception {
    private final String _key;

    public KeyNotFoundException (final String key) {
        _key = key;
    }

    /**
     * Returns the key that was not found.
     */
    public String getKey () {
        return (_key);
    }
}