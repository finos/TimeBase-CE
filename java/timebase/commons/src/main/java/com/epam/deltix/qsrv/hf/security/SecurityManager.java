package com.epam.deltix.qsrv.hf.security;

import com.epam.deltix.util.security.AuthorizationController;

import java.security.AccessControlException;
import java.security.Principal;

public interface SecurityManager {

    /**
     * Returns current user id.
     * @return current user id
     */
    String getCurrentUser();

    Principal getCurrentPrincipal();

    /**
     * Determines whether the access request indicated by the specified permission
     * should be allowed or denied, based on the security policy currently in effect.
     *
     * @param permissionId the requested permission
     * @param targetUserId user against the request permission is checked
     * @throws AccessControlException -
     *      if the specified permission is not permitted, based on the current security policy.
     */
    void checkPermission(String permissionId, String targetUserId) throws AccessControlException;
    void checkPermission(String permissionId, AuthorizationController.ProtectedResource resource) throws AccessControlException;

    /**
     * Test current user for system permission
     *
     * @param permissionId the requested permission
     * @throws AccessControlException
     *      if the specified permission is not permitted, based on the current security policy.
     */
    void checkSystemPermission(String permissionId) throws AccessControlException;

    /**
     * Tests if the access request indicated by the specified permission is allowed for the target user.
     *
     * @param permissionId the requested permission
     * @param targetUserId user against the request permission is checked
     * @return <code>true</code> if the access request indicated by the specified permission is allowed for the target user.
     */
    boolean hasPermission(String permissionId, String targetUserId);
    boolean hasPermission(String permissionId, AuthorizationController.ProtectedResource resource);

    /**
     * @param permissionId the requested system permission
     * @return <code>true</code> if the access request indicated by the specified permission is allowed for the target user.
     */
    boolean hasSystemPermission(String permissionId);
}
