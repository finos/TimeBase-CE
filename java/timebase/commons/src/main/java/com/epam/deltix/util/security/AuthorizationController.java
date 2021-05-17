/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
