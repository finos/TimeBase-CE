package com.epam.deltix.util.security;

/**
 * Both LDAP and Simple file user directories support password validation
 */
public interface AuthenticatingUserDirectory extends UserDirectory, AuthenticationController {

    /**
     * Changes user password.
     * @param user username.
     * @param oldPassword old password.
     * @param newPassword new password.
     */
    void changePassword(String user, String oldPassword, String newPassword);
}
