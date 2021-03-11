package com.epam.deltix.util.security;

public class AuthenticationException extends SecurityException {

    public AuthenticationException(String s) {
        super(s);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
