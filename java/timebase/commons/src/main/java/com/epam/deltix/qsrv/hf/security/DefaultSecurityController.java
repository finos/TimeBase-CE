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
package com.epam.deltix.qsrv.hf.security;

import java.security.AccessControlException;
import java.security.Principal;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.qsrv.hf.security.rules.AccessControlEntry;
import com.epam.deltix.qsrv.hf.security.rules.DefaultAuthorizationController;
import com.epam.deltix.qsrv.hf.security.rules.ManagedAuthorizationController;
import com.epam.deltix.util.lang.Factory;
import com.epam.deltix.util.security.*;
import com.epam.deltix.util.security.SecurityController;
import com.epam.deltix.util.time.GlobalTimer;
import com.epam.deltix.util.time.Interval;

public class DefaultSecurityController implements SecurityController, ManagedAuthorizationController, SecurityReloadNotifier {
    private static final Log LOGGER = SecurityConfigurator.LOGGER;
    private static final String LOGPREFIX = SecurityConfigurator.LOGPREFIX;

    private final Factory<AuthenticatingUserDirectory> userDirectoryFactory;
    private final AccessControlRulesFactory rulesFactory;

    private volatile AuthenticatingUserDirectory authenticationController;
    private volatile ManagedAuthorizationController authorizationController;

    private final CopyOnWriteArrayList<SecurityReloadListener> reloadListeners = new CopyOnWriteArrayList<>();

    DefaultSecurityController(Factory<AuthenticatingUserDirectory> userDirectoryFactory, AccessControlRulesFactory rulesFactory, Interval updateInterval) {
        this.userDirectoryFactory = userDirectoryFactory;
        this.rulesFactory = rulesFactory;

        if (updateInterval != null && !updateInterval.isZero()) {
            LOGGER.info(LOGPREFIX + "Schedule configuration reloading in " + updateInterval.toHumanString());
            long delay = updateInterval.toMilliseconds();
            GlobalTimer.INSTANCE.schedule(new RefreshTask(), delay, delay);
        }

        reload();
    }

    @Override
    public void checkPermission(Principal principal, String permission, ProtectedResource resource) throws AccessControlException {
        authorizationController.checkPermission(principal, permission, resource);
    }

    @Override
    public void checkPermission(Principal principal, String permission) throws AccessControlException {
        authorizationController.checkPermission(principal, permission);
    }

    @Override
    public boolean hasPermission(Principal principal, String permission, ProtectedResource resource) {
        return authorizationController.hasPermission(principal, permission, resource);
    }

    @Override
    public boolean hasPermission(Principal principal, String permission) {
        return authorizationController.hasPermission(principal, permission);
    }

    @Override
    public boolean hasPermissionOverPrincipal(Principal principal, String permission, String anotherPrincipal) {
        return authorizationController.hasPermissionOverPrincipal(principal, permission, anotherPrincipal);
    }

    @Override
    public Principal authenticate(String name, String pass) {
        return authenticationController.authenticate(name, pass);
    }

    @Override
    public Principal getUser(String name) {
        return authenticationController.getUser(name);
    }

    @SuppressWarnings("unused") //used by JSP
    public final void reload() {
        authenticationController = userDirectoryFactory.create();
        authorizationController = new DefaultAuthorizationController(authenticationController, rulesFactory.create(authenticationController));
        fireReloaded();
        LOGGER.info(LOGPREFIX + "Configuration has been reloaded");
    }

    @Override
    public void visit(PermissionVisitor visitor) {
        authorizationController.visit(visitor);
    }


    @Override
    public List<AccessControlEntry> getEffectivePermissions() {
        return authorizationController.getEffectivePermissions();
    }

    @Override
    public Principal impersonate(String name, String pass, String delegate) {
        Principal principal = authenticationController.authenticate(name, pass);

        authorizationController.checkPermission(principal, TimeBasePermissions.IMPERSONATE_PERMISSION);

        return authenticationController.getUser(delegate);
    }

    @Override
    public void                 addReloadListener(SecurityReloadListener listener) {
        reloadListeners.addIfAbsent(listener);
    }

    @Override
    public void                 removeReloadListener(SecurityReloadListener listener) {
        reloadListeners.remove(listener);
    }

    private void                fireReloaded() {
        for (SecurityReloadListener listener : reloadListeners)
            listener.reloaded();
    }

    @Override
    public void changePassword(String user, String oldPassword, String newPassword) {
        authenticationController.changePassword(user, oldPassword, newPassword);
    }

    //////////////////////////// HELPER CLASSES /////////////////////////////

    private class RefreshTask extends TimerTask {
        @Override
        public void run() {
            try {
                reload();
            } catch (Throwable e) {
                LOGGER.error("Failed to reload configuration: " + e);
            }
        }
    }
}
