/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.security;

import com.epam.deltix.qsrv.hf.security.rules.AccessControlEntry;

import java.security.Principal;
import java.util.List;

public final class SecurityContext implements SecurityProvider, SecurityController {
    private static final ThreadLocal<Principal> CURRENT_USER = new ThreadLocal<>();

    private static final ThreadLocal<MutablePrincipal> MUTABLE_USER = new ThreadLocal<MutablePrincipal>() {
        @Override
        protected MutablePrincipal initialValue() {
            return new MutablePrincipal();
        }
    };

    private final ContextSecurityManager securityManager;

    public SecurityContext(com.epam.deltix.util.security.SecurityController permissionResolver) {
        securityManager = new ContextSecurityManager(permissionResolver);
    }

    public static SecurityContext create(com.epam.deltix.util.security.SecurityController controller) {
        return controller != null ? new SecurityContext(controller) : null;
    }

    @Override
    public String setCurrentUser(String user) {
        Principal prevUser = CURRENT_USER.get();
        MutablePrincipal principal = null;
        if (user != null) {
            principal = MUTABLE_USER.get();
            principal.setName(user);
        }
        CURRENT_USER.set(principal);
        return prevUser != null ?  prevUser.getName() : null;
    }

    @Override
    public String getCurrentUser() {
        Principal principal = getCurrentPrincipal();
        return principal != null ?  principal.getName() : null;
    }

    @Override
    public Principal setCurrentPrincipal(Principal user) {
        Principal prevUser = CURRENT_USER.get();
        CURRENT_USER.set(user);
        return prevUser;
    }

    @Override
    public Principal getCurrentPrincipal() {
        return CURRENT_USER.get();
    }

    @Override
    public SecurityManager getSecurityManager() {
        return CURRENT_USER.get() != null ? securityManager : null;
    }

    @Override
    public List<AccessControlEntry> getEffectivePermissions() {
        return securityManager.getEffectivePermissions();
    }

    @Override
    public void reloadPermissions() {
        securityManager.reloadPermissions();
    }


    ///////////////////////// HELPER CLASSES /////////////////////

    private static final class ContextSecurityManager extends SecurityManagerBase {
        private ContextSecurityManager(com.epam.deltix.util.security.SecurityController securityController) {
            super(securityController);
        }

        @Override
        public String getCurrentUser() {
            return CURRENT_USER!= null? CURRENT_USER.get().getName() : null ;
        }

        @Override
        public Principal getCurrentPrincipal() {
            return CURRENT_USER.get();
        }

    }

    private static final class MutablePrincipal implements Principal {
        private String name;

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            if (o instanceof Principal) {
                String principalName = ((Principal) o).getName();
                return principalName != null && principalName.equalsIgnoreCase(name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.toLowerCase().hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}