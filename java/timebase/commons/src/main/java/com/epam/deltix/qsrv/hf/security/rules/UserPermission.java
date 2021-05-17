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
package com.epam.deltix.qsrv.hf.security.rules;

import com.epam.deltix.util.security.AccessControlRule;
import com.epam.deltix.util.security.AuthorizationController;
import com.epam.deltix.util.text.WildcardMatcher;

import java.util.*;
import java.util.regex.Pattern;

/** Set of permissions defined for a specific user */
final class UserPermissionsSet {

    private final Map<String, AllowDenyUserPermission> resourcesByPermission = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    UserPermission getOrCreate(AccessControlRule.RuleEffect ruleEffect, String permissionId) {
        AllowDenyUserPermission result = resourcesByPermission.get(permissionId);
        if (result == null) {
            result = new AllowDenyUserPermission();
            resourcesByPermission.put(permissionId, result);
        }
        return result.getOrCreate(ruleEffect);
    }

    AllowDenyUserPermission get(String permissionId) {
        return resourcesByPermission.get(permissionId);
    }


    void simplifyPermissionRules() {
        expandAnyPermission();

        for (AllowDenyUserPermission p : resourcesByPermission.values())
            p.simplify();
    }

    // If user has "*" permissions over some resources let's combine them with explicit permissions.
    // This will help us at runtime. Imagine we have the following set of permissions defined:
    //   {Allow, joe, *, joe}
    //   {Allow, joe, READ, sam}
    // This method will expand the set as follows:
    //   {Allow, joe, *, joe}
    //   {Allow, joe, READ, sam}
    //   {Allow, joe, READ, joe} <== expanded
    // At run time when we check permission "can joe READ abc" we can perform just one
    // lookup: "what resources joe can READ" (and won't need the second lookup "... or what resources joe can access with *any* permission")
    // Note: that we must keep original permission {allow joe * joe} to check permissions not mentioned in the set (e.g. WRITE) or custom permissions.
    private void expandAnyPermission() {
        AllowDenyUserPermission anyPermission = resourcesByPermission.get(DefaultAuthorizationController.ANY_PERMISSION);
        if (anyPermission != null) {
            for (Map.Entry<String, AllowDenyUserPermission> entry : resourcesByPermission.entrySet()) {
                String permissionId = entry.getKey();
                if ( ! permissionId.equals(DefaultAuthorizationController.ANY_PERMISSION)) {
                    entry.getValue().add(anyPermission);
                }
            }
        }
    }


    void visit(ManagedAuthorizationController.PermissionVisitor visitor, String userId) {
        if (resourcesByPermission != null) {
            for (Map.Entry<String, AllowDenyUserPermission> entry : resourcesByPermission.entrySet()) {
                String permissionId = entry.getKey();
                entry.getValue().visit(visitor, userId, permissionId);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder (128);
        visit(new ManagedAuthorizationController.PermissionVisitor() {
            @Override
            public void visit(AccessControlRule.RuleEffect ruleEffect, String user, String permission, String resource, AccessControlRule.ResourceType type) {
                if (sb.length() > 0)
                    sb.append('\n');

                sb.append(ruleEffect).append(' ')
                    .append(permission).append(',')
                  .append(resource);

                if (type != null)
                    sb.append(':').append(type);
            }
        }, null);

        return sb.toString();
    }
}

/** Defines ALLOW and DENY sets of resources for specific permission (defined for specific user) */
final class AllowDenyUserPermission {
   UserPermission allowPermission;
   UserPermission denyPermission;

    UserPermission getOrCreate(AccessControlRule.RuleEffect ruleEffect) {
        if (ruleEffect == AccessControlRule.RuleEffect.Allow) {
            if (allowPermission == null)
                allowPermission = new UserPermission();
            return allowPermission;
        } else {
            if (denyPermission == null)
                denyPermission = new UserPermission();
            return denyPermission;
        }
    }

    void add(AllowDenyUserPermission permission) {
        if (permission.allowPermission != null)
            getOrCreate(AccessControlRule.RuleEffect.Allow).add(permission.allowPermission);
        if (permission.denyPermission != null)
            getOrCreate(AccessControlRule.RuleEffect.Deny).add(permission.denyPermission);
    }

    void simplify() {
        if (allowPermission != null)
            allowPermission.simplify();
        if (denyPermission != null)
            denyPermission.simplify();

        if (allowPermission != null && denyPermission != null) {
            if (allowPermission.removeAll(denyPermission)) // DENY is stronger than ALLOW
                allowPermission = null;
        }
    }

    void visit(ManagedAuthorizationController.PermissionVisitor visitor, String userId, String permissionId) {
        if (allowPermission != null)
            allowPermission.visit(visitor, userId, permissionId, AccessControlRule.RuleEffect.Allow);
        if (denyPermission != null)
            denyPermission.visit(visitor, userId, permissionId, AccessControlRule.RuleEffect.Deny);
    }

    boolean match(AuthorizationController.ProtectedResource resource) {
        if (allowPermission != null)
            if (allowPermission.match(resource))
                return (denyPermission == null) || ! denyPermission.match(resource);

        return false;
    }

    boolean match(String resource, AccessControlRule.ResourceType type) {
        if (allowPermission != null)
            if (allowPermission.match(resource, type))
                return (denyPermission == null) || ! denyPermission.match(resource, type);

        return false;
    }


    boolean matchAny() {
        if (allowPermission != null)
            if (allowPermission.matchAny())
                return (denyPermission == null) || ! denyPermission.matchAny();

        return false;
    }

    @Override
    public String toString() {
        return
            "ALLOW:{" + allowPermission +
            "} DENY:{" + denyPermission +
            '}';
    }
}

/** Describes single permission of a specific user (contains list of resources that this permission applies to) */
final class UserPermission {

    private static final AccessControlRule.ResourceType [] RESOURCE_TYPES = AccessControlRule.ResourceType.values();
    private static final int NUM_RESOURCE_TYPES = RESOURCE_TYPES.length;

    private boolean matchesWithAnyResource; // DefaultAccessController.ANY_RESOURCE
    private Set<String>[] textResources;
    private List<String>[] resourceWildcards;
    private List<Pattern>[] resourcePatterns;

    void add(String resource, AccessControlRule.ResourceType resourceType, AccessControlRule.ResourceFormat resourceFormat) {
        if (resourceFormat == AccessControlRule.ResourceFormat.Text) {
            if (resource.equals(DefaultAuthorizationController.ANY_RESOURCE)) {
                matchesWithAnyResource = true;
            } else {
                addText(resource, resourceType);
            }
        } else if (resourceFormat == AccessControlRule.ResourceFormat.Wildcard) {
            addWildcard(resource, resourceType);
        } else if (resourceFormat == AccessControlRule.ResourceFormat.RegEx) {
            addRegex(resource, resourceType);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unchecked")
    private void addText(String resource, AccessControlRule.ResourceType resourceType) {
        assert ! resource.equals(DefaultAuthorizationController.ANY_RESOURCE);
        if (textResources == null)
            textResources = (Set<String>[]) new Set<?>[NUM_RESOURCE_TYPES];

        final int ordinal = resourceType.ordinal();
        if (textResources[ordinal] == null)
            textResources[ordinal] = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        textResources[ordinal] .add(resource);
    }

    @SuppressWarnings("unchecked")
    private void addWildcard (String wildcard, AccessControlRule.ResourceType resourceType) {
        if (resourceWildcards == null)
            resourceWildcards = (List<String>[]) new List<?>[NUM_RESOURCE_TYPES];

        final int ordinal = resourceType.ordinal();
        if (resourceWildcards[ordinal] == null)
            resourceWildcards[ordinal] = new ArrayList<>();

        resourceWildcards[ordinal] .add(wildcard);
    }

    private void addRegex (String pattern, AccessControlRule.ResourceType resourceType) {
        addRegex (Pattern.compile(pattern), resourceType);
    }

    @SuppressWarnings("unchecked")
    private void addRegex (Pattern pattern, AccessControlRule.ResourceType resourceType) {
        if (resourcePatterns == null)
            resourcePatterns = (List<Pattern>[]) new List<?>[NUM_RESOURCE_TYPES];

        final int ordinal = resourceType.ordinal();
        if (resourcePatterns[ordinal] == null)
            resourcePatterns[ordinal] = new ArrayList<>();

        resourcePatterns[ordinal].add(pattern);
    }

    void add(UserPermission other) {
        this.matchesWithAnyResource |= other.matchesWithAnyResource;

        if (other.textResources != null) {
            for (int ordinal = 0; ordinal < NUM_RESOURCE_TYPES; ordinal++) {
                if (other.textResources[ordinal] != null)
                    for (String resource : other.textResources[ordinal])
                        addText(resource, RESOURCE_TYPES[ordinal]);
            }
        }
        if (other.resourceWildcards != null) {
            for (int ordinal = 0; ordinal < NUM_RESOURCE_TYPES; ordinal++) {
                if (other.resourceWildcards[ordinal] != null)
                    for (String wildcard : other.resourceWildcards[ordinal])
                        addWildcard(wildcard, RESOURCE_TYPES[ordinal]);
            }
        }
        if (other.resourcePatterns != null) {
            for (int ordinal = 0; ordinal < NUM_RESOURCE_TYPES; ordinal++) {
                if (other.resourcePatterns[ordinal] != null)
                    for (Pattern pattern : other.resourcePatterns[ordinal])
                        addRegex(pattern, RESOURCE_TYPES[ordinal]);
            }
        }
    }

    void simplify () {
        if (matchesWithAnyResource) {
            textResources = null;
            resourceWildcards = null;
            resourcePatterns = null;
        }
    }

    boolean match(AuthorizationController.ProtectedResource resource) {
        if (matchesWithAnyResource)
            return true;

        if (resource instanceof AuthorizationController.OwnedProtectedResource) {
            String owner = ((AuthorizationController.OwnedProtectedResource) resource).getOwner();
            if (owner != null && _match(owner, AccessControlRule.ResourceType.Principal))
                return true;
        }
        if (resource instanceof AuthorizationController.NamedProtectedResource) {
            String name = ((AuthorizationController.NamedProtectedResource) resource).getKey();
            if (name != null && _match(name, AccessControlRule.ResourceType.Stream))
                return true;
        }
        return false; // unsupported type of resource
    }

    boolean match(String resource, AccessControlRule.ResourceType resourceType) {
        if (matchesWithAnyResource)
            return true;
        return _match(resource, resourceType);
    }

    private boolean _match(String resource, AccessControlRule.ResourceType resourceType) {

        final int ordinal = resourceType.ordinal();
        if (textResources != null && textResources[ordinal] != null && textResources[ordinal].contains(resource))
            return true;

        if (resourceWildcards != null) {
            List<String> wildcards = resourceWildcards[ordinal];
            if (wildcards != null)
                for (int i = 0; i < wildcards.size(); i++) {
                    String wildcard = wildcards.get(i);
                    if (WildcardMatcher.match(resource, wildcard))
                        return true;
                }
        }

        if (resourcePatterns != null) {
            List<Pattern> patterns = resourcePatterns[ordinal];
            if (patterns != null)
                for (int i = 0; i < patterns.size(); i++) {
                    Pattern pattern = patterns.get(i);
                    if (pattern.matcher(resource).matches()) //Pattern is immutable and are safe for use by multiple concurrent threads.
                        return true;
                }
        }

        return false;
    }

    /** @return true if user has permission to see any resource */
    boolean matchAny () {
        return matchesWithAnyResource;
    }

    void visit(ManagedAuthorizationController.PermissionVisitor visitor, String userId, String permissionId, AccessControlRule.RuleEffect ruleEffect) {
        if (matchesWithAnyResource) {
            visitor.visit(ruleEffect, userId, permissionId, DefaultAuthorizationController.ANY_RESOURCE, null); // null = any resource type
        } else {
            for (int ordinal = 0; ordinal < NUM_RESOURCE_TYPES; ordinal++) {
                AccessControlRule.ResourceType resourceType = RESOURCE_TYPES[ordinal];
                if (textResources != null && textResources[ordinal] != null) {
                    for (String text : textResources[ordinal])
                        visitor.visit(ruleEffect, userId, permissionId, text, resourceType);
                }
                if (resourceWildcards != null && resourceWildcards[ordinal] != null) {
                    for (String wildcard : resourceWildcards[ordinal])
                        visitor.visit(ruleEffect, userId, permissionId, wildcard, resourceType);
                }
                if (resourcePatterns != null && resourcePatterns[ordinal] != null) {
                    for (Pattern regex : resourcePatterns[ordinal])
                        visitor.visit(ruleEffect, userId, permissionId, regex.toString(), resourceType);
                }
            }
        }
    }

    // removes given set of resources from the current resource set
    boolean removeAll(UserPermission deniedResources) {
        if (deniedResources.matchesWithAnyResource)
            return true;

        removeAll(this.textResources, deniedResources.textResources);
        removeAll(this.resourceWildcards, deniedResources.resourceWildcards);
        removeAll(this.resourcePatterns, deniedResources.resourcePatterns);
        return isEmpty();
    }

    private boolean isEmpty() {
        return ( ! matchesWithAnyResource ) && isEmpty(textResources) && isEmpty(resourceWildcards) && isEmpty(resourcePatterns);
    }

    private static boolean isEmpty(Collection<?>[] c) {
        if (c != null) {
            for (Collection<?> cc : c)
                if (cc != null && ! cc.isEmpty())
                    return false;
        }
        return true;
    }

    private static void removeAll(Collection<?>[] allow, Collection<?>[] deny) {
        if (allow != null && deny != null) {
            for (int ordinal = 0; ordinal < NUM_RESOURCE_TYPES; ordinal++) {
                Collection<?> allowed = allow[ordinal];
                Collection<?> denied = deny[ordinal];
                if (allowed != null && denied != null)
                    allowed.removeAll(denied);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder (128);
        visit(new ManagedAuthorizationController.PermissionVisitor() {
            @Override
            public void visit(AccessControlRule.RuleEffect ruleEffect, String user, String permission, String resource, AccessControlRule.ResourceType type) {
                if (sb.length() > 0)
                    sb.append('\n');

                sb.append(resource);
                if (type != null)
                    sb.append(':').append(type);

                if (ruleEffect != null)
                    sb.append(' ').append(ruleEffect);
            }
        }, null, null, null);

        return sb.toString();
    }

}