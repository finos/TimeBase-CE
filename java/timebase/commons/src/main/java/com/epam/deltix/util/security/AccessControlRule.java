package com.epam.deltix.util.security;

import java.security.Principal;

/**
 *
 */
public interface AccessControlRule {

    public static final ResourceFormat DEFAULT_RESOURCE_FORMAT = ResourceFormat.Text;
    public static final ResourceType DEFAULT_RESOURCE_TYPE = ResourceType.Principal;
    public static final RuleEffect DEFAULT_RULE_EFFECT = RuleEffect.Allow;

    enum ResourceFormat {
        /** Rule defines literally resource ID */
        Text,

        /** Rule defines regular expression pattern that matches with actual resource ID */
        RegEx,

        /** Resource ID that contains wildcards (e.g. "Trader*") */
        Wildcard
    }

    enum RuleEffect {
        Allow,
        Deny
    }


    enum ResourceType {
        /** Resource identifies a stream (via stream key) */
        Stream,

        /** Resource identifies a user or group (via user LDAP id) */
        Principal
    }

    /** Who is performing action that requires permission */
    Principal getPrincipal();

    /** What permission is requested */
    String getPermission();

    /** What is the subject/object of the action */
    String getResource();

    /** @return resource type (never NULL) */
    ResourceType getResourceType();

    /** @return resource format (never NULL) */
    ResourceFormat getResourceFormat();

    RuleEffect getEffect();
}
