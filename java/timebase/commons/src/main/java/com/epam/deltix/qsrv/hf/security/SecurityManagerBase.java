package com.epam.deltix.qsrv.hf.security;


import com.epam.deltix.qsrv.hf.security.rules.AccessControlEntry;
import com.epam.deltix.util.security.AuthorizationController;
import com.epam.deltix.qsrv.hf.security.rules.ManagedAuthorizationController;
import com.epam.deltix.util.security.SecurityController;

import java.security.AccessControlException;
import java.util.List;

/**
 * Date: Feb 11, 2011
 * @author Nickolay Dul
 */
public abstract class SecurityManagerBase implements com.epam.deltix.qsrv.hf.security.SecurityManager, com.epam.deltix.qsrv.hf.security.SecurityController {
    private final SecurityController securityController;

    public SecurityManagerBase(SecurityController securityController) {
        this.securityController = securityController;
    }

    @Override
    public void checkPermission(String permissionId, String targetUserId) throws AccessControlException {
        if (!securityController.hasPermissionOverPrincipal(getCurrentPrincipal(), permissionId, targetUserId))
            throw new AccessControlException("Permission denied: You are not authorized to perform operation \"" + permissionId + "\" on resource owned by \"" + targetUserId + '"');
    }

    @Override
    public void checkPermission(String permissionId, AuthorizationController.ProtectedResource resource) throws AccessControlException {
        if (!securityController.hasPermission(getCurrentPrincipal(), permissionId, resource))
            if (resource instanceof AuthorizationController.NamedProtectedResource)
                throw new AccessControlException("Permission denied: You are not authorized to perform operation \"" + permissionId + "\" on resource \"" + ((AuthorizationController.NamedProtectedResource)resource).getKey() + '"');
            else
            if (resource instanceof AuthorizationController.OwnedProtectedResource)
                throw new AccessControlException("Permission denied: You are not authorized to perform operation \"" + permissionId + "\" on resource owned by \"" + ((AuthorizationController.OwnedProtectedResource)resource).getOwner() + '"');
            else
                throw new AccessControlException("Permission denied: You are not authorized to perform operation \"" + permissionId + "\" on resource \"" + resource + '"');
    }

    @Override
    public void checkSystemPermission(String permissionId) {
        if (!securityController.hasPermission(getCurrentPrincipal(), permissionId))
            throw new AccessControlException("Permission denied: You are not authorized to perform system-level operation \"" + permissionId + "\".");
    }

    @Override
    public boolean hasPermission(String permissionId, String targetUserId) {
        return securityController.hasPermissionOverPrincipal(getCurrentPrincipal(), permissionId, targetUserId);
    }

    @Override
    public boolean hasPermission(String permissionId, AuthorizationController.ProtectedResource resource) {
        return securityController.hasPermission(getCurrentPrincipal(), permissionId, resource);
    }

    @Override
    public boolean hasSystemPermission(String permissionId) {
        return securityController.hasPermission(getCurrentPrincipal(), permissionId);
    }

    @Override
    public List<AccessControlEntry> getEffectivePermissions() {
        if (securityController instanceof ManagedAuthorizationController)
            return ((ManagedAuthorizationController)securityController).getEffectivePermissions();
        return null;
    }

    @Override
    public void reloadPermissions() {
        if (securityController instanceof DefaultSecurityController)
            ((DefaultSecurityController) securityController).reload();
    }
}
