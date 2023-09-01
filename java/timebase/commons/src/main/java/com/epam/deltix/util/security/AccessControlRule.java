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