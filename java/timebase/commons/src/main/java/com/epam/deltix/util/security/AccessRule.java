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

public final class AccessRule implements AccessControlRule {
    private final RuleEffect effect;
    private final Principal principal;
    private final String permission;
    private final String resource;
    private final ResourceType resourceType;
    private final ResourceFormat resourceFormat;

    public AccessRule(RuleEffect effect, Principal principal, String permission,
                      String resource, ResourceType resourceType, ResourceFormat resourceFormat) {
        this.effect = effect;
        this.principal = principal;
        this.permission = permission;
        this.resource = resource;
        this.resourceType = resourceType;
        this.resourceFormat = resourceFormat;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    @Override
    public String getResource() {
        return resource;
    }

    @Override
    public ResourceType getResourceType() {
        return resourceType;
    }

    @Override
    public ResourceFormat getResourceFormat() {
        return resourceFormat;
    }

    @Override
    public RuleEffect getEffect() {
        return effect;
    }

    @Override
    public String toString() {
        return "AccessRule{" +
                "effect=" + effect +
                ", principal=" + principal +
                ", permission='" + permission + '\'' +
                ", resource='" + resource + '\'' +
                ", resourceType=" + resourceType +
                ", resourceFormat=" + resourceFormat +
                '}';
    }
}
