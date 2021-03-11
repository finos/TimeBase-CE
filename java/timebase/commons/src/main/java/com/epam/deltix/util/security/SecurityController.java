package com.epam.deltix.util.security;

import java.security.Principal;

/**
 * Component that can authenticate, authorize, and impersonate users.
 */
public interface SecurityController extends AuthenticationController, AuthorizationController {

    Principal impersonate(String name, String pass, String delegate);

    /**
     * Changes user password.
     * @param user username.
     * @param oldPassword old password.
     * @param newPassword new password.
     */
    void changePassword(String user, String oldPassword, String newPassword);
}
