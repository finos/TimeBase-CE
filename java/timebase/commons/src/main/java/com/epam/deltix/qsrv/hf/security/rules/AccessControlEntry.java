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

public final class AccessControlEntry {
    private final String currentUser;
    private final String permission;
    private final String targetUser;
    private final AccessControlRule.RuleEffect ruleEffect;

    public AccessControlEntry(AccessControlRule.RuleEffect ruleEffect, String currentUser, String permission, String targetUser) {
        this.ruleEffect = ruleEffect;
        this.currentUser = currentUser;
        this.permission = permission;
        this.targetUser = targetUser;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public String getPermission() {
        return permission;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public AccessControlRule.RuleEffect getRuleEffect() {
        return ruleEffect;
    }

    @Override
    public String toString() {
        return "AccessControlEntry{" +
                "ruleEffect=" + ruleEffect +
                ",currentUser='" + currentUser + '\'' +
                ", permission='" + permission + '\'' +
                ", targetUser='" + targetUser + '\'' +
                '}';
    }
}