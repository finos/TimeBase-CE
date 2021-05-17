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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.epam.deltix.util.security.AccessControlRule.ResourceFormat;
import com.epam.deltix.util.security.AccessControlRule.RuleEffect;
import com.epam.deltix.util.security.AccessControlRule.ResourceType;

public class RuleEntry extends Entry {
    private static final ResourceEntry[] EMPTY_RESOURCE_ARRAY = new ResourceEntry[0];

    private RuleEffect effect = RuleEffect.Deny;
    private final List<Principal> principals = new ArrayList<>(2);
    private final List<ResourceEntry> resources = new ArrayList<>(2);
    private final List<String> permissions = new ArrayList<>(2);

    public RuleEntry(String id) {
        super(id);
    }

    public RuleEffect getEffect() {
        return effect;
    }

    public void setEffect(RuleEffect effect) {
        this.effect = effect;
    }

    public Principal[] getPrincipals() {
        return principals.toArray(new Principal[principals.size()]);
    }

    public ResourceEntry[] getResources() {
        if (resources.isEmpty())
            return EMPTY_RESOURCE_ARRAY;
        return resources.toArray(new ResourceEntry[resources.size()]);
    }

    public String[] getPermissions() {
        return permissions.toArray(new String[permissions.size()]);
    }

    public void setPermissions(String[] permissions) {
        Collections.addAll(this.permissions, permissions);
    }

    public void addResource(ResourceEntry resource) {
        resources.add(resource);
    }

    public void addPrincipal(Principal principal) {
        principals.add(principal);
    }

    public void addPermission(String permission) {
        permissions.add(permission);
    }

    public List<AccessControlRule> listAccessRules() {
        List<AccessControlRule> result = new ArrayList<>(permissions.size() * principals.size() * (resources.isEmpty() ? 1 : resources.size()));
        for (int i = 0; i < principals.size(); i++) {
            Principal principal = principals.get(i);
            for (int j = 0; j < permissions.size(); j++) {
                String permission = permissions.get(j);
                if (resources.size() > 0) {
                    for (int k = 0; k < resources.size(); k++) {
                        ResourceEntry resource = resources.get(k);
                        result.add(new AccessRule(effect, principal, permission, resource.getResource(), resource.getResourceType(), resource.getResourceFormat()));
                    }
                } else
                    result.add(new AccessRule(effect, principal, permission, null, ResourceType.Principal, ResourceFormat.Text)); // system UAC rules don't have resource argument
            }
        }
        return result;
    }

    public boolean isEmpty() {
        return permissions == null || permissions.isEmpty() || principals == null || principals.isEmpty();
    }

    @Override
    public String toString() {
        return getId();
    }
}
