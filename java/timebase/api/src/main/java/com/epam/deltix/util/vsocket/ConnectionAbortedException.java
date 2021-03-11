package com.epam.deltix.util.vsocket;

import java.io.IOException;

/**
 * Date: Apr 14, 2010
 * Time: 5:45:37 PM
 */
public class ConnectionAbortedException extends IOException {

    public ConnectionAbortedException() {
    }

    public ConnectionAbortedException(String message) {
        super(message);
    }

    public ConnectionAbortedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionAbortedException(Throwable cause) {
        super(cause);
    }
}
