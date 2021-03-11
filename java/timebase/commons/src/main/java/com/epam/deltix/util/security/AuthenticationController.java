package com.epam.deltix.util.security;

import java.security.Principal;

/**
 *
 */
public interface AuthenticationController {
    Principal authenticate(String name, String pass);
    Principal getUser(String name);
}