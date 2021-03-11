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
