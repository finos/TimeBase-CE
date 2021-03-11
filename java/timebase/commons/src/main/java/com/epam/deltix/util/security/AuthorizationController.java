package com.epam.deltix.util.security;

import java.security.AccessControlException;
import java.security.Principal;

/**
 * Verifies user permissions over protected resources
 */
public interface AuthorizationController {

    interface ProtectedResource {
    }

    interface OwnedProtectedResource extends ProtectedResource {
        String getOwner();
    }

    interface NamedProtectedResource extends ProtectedResource {
        String getKey();
    }

    /**
     * Checks permission of given user to perform given action on given resource (e.g. READ given stream)
     * @param principal Who is doing the Action?
     * @param permission What Action?
     * @param resource On which resource?
     * @throws  AccessControlException if user is not authorized for given action
     */
    void        checkPermission(Principal principal, String permission, ProtectedResource resource)
        throws AccessControlException;

    /**
     * Checks permission of given user to perform given <i>system</i> action (e.g. CONNECT to TimeBase)
     * @param principal Who is doing the Action
     * @param permission What Action
     * @throws  AccessControlException if user is not authorized for given action
     */
    void        checkPermission(Principal principal, String permission)
        throws AccessControlException;


    /**
     * Checks permission of given user to perform given action on given resource (e.g. READ given stream)
     * @param principal Who is doing the Action
     * @param permission What Action
     * @param resource On which resource
     * @return false if user is not authorized for given action
     */
    boolean     hasPermission(Principal principal, String permission, ProtectedResource resource);

    /**
     * Checks permission of given user to perform given <i>system</i> action (e.g. CONNECT to TimeBase)
     * @param principal Who is doing the Action
     * @param permission What Action
     * @return false if user is not authorized for given action
     */
    boolean     hasPermission(Principal principal, String permission);

    /**
     * Checks permission of given user to perform given action on given resource (e.g. READ given stream)
     * @param principal Who is doing the Action?
     * @param permission What Action?
     * @param anotherPrincipal On resource owned by which principal?
     * @return false given principal has given permission over resources owned by anotherPrincipal
     */
    boolean        hasPermissionOverPrincipal(Principal principal, String permission, String anotherPrincipal);


}
